package com.velox.order.live;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;
import org.springframework.web.context.request.async.DeferredResult;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * Live updates (Observer: order changes are pushed to whoever follows them). Each signed-in app keeps one Server-Sent Events stream open (GET /api/events) and gets
 * "order", "offer" and "driver-location" events as they happen.
 * Some proxies hold SSE back (Cloudflare quick tunnels), so every event is also kept a few minutes in a
 * per-account mailbox that apps can long-poll instead (GET /api/events/poll?after=…).
 */
@Component
public class EventHub {

    public record Event(long seq, String event, Object data) {}

    public record Poll(long last, List<Event> events) {}

    private static final int MAILBOX_SIZE = 100;
    private static final long MAILBOX_MILLIS = TimeUnit.MINUTES.toMillis(5);
    private static final long POLL_WAIT_MILLIS = TimeUnit.SECONDS.toMillis(20);

    private final Map<Long, List<SseEmitter>> streams = new ConcurrentHashMap<>();
    private final Map<Long, Deque<Stamped>> mailboxes = new ConcurrentHashMap<>();
    private final Map<Long, List<Waiter>> waiters = new ConcurrentHashMap<>();
    private final AtomicLong seq = new AtomicLong();
    private final ScheduledExecutorService heartbeat = Executors.newSingleThreadScheduledExecutor(r -> {
        Thread t = new Thread(r, "sse-heartbeat");
        t.setDaemon(true);
        return t;
    });

    private record Stamped(Event event, long at) {}

    private record Waiter(long after, DeferredResult<Poll> result) {}

    public EventHub() {
        // a comment line every 25 s keeps proxies and phones from closing idle streams
        heartbeat.scheduleAtFixedRate(() -> streams.keySet().forEach(id -> stream(id, null, null)), 25, 25, TimeUnit.SECONDS);
    }

    public SseEmitter subscribe(long userId) {
        SseEmitter emitter = new SseEmitter(0L);
        streams.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);
        Runnable remove = () -> {
            List<SseEmitter> list = streams.get(userId);
            if (list != null) {
                list.remove(emitter);
            }
        };
        emitter.onCompletion(remove);
        emitter.onTimeout(remove);
        emitter.onError(e -> remove.run());
        stream(userId, "hello", Map.of("ok", true));
        return emitter;
    }

    /**
     * Events newer than [after], answered at once if there are any, otherwise as soon as one arrives
     * (or empty after 20 s). Without [after] it just returns the current position to start from.
     */
    public DeferredResult<Poll> poll(long userId, Long after) {
        DeferredResult<Poll> result = new DeferredResult<>(POLL_WAIT_MILLIS + 5000);
        if (after == null || after < 0) {
            result.setResult(new Poll(seq.get(), List.of()));
            return result;
        }
        List<Event> ready = since(userId, after);
        if (!ready.isEmpty()) {
            result.setResult(new Poll(ready.get(ready.size() - 1).seq(), ready));
            return result;
        }
        Waiter waiter = new Waiter(after, result);
        List<Waiter> list = waiters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>());
        list.add(waiter);
        Runnable remove = () -> list.remove(waiter);
        result.onCompletion(remove);
        result.onTimeout(() -> result.setResult(new Poll(Math.max(after, 0), List.of())));
        heartbeat.schedule(() -> result.setResult(new Poll(after, List.of())), POLL_WAIT_MILLIS, TimeUnit.MILLISECONDS);
        return result;
    }

    public void sendTo(Collection<Long> userIds, String event, Object data) {
        userIds.stream().distinct().forEach(id -> {
            Event e = new Event(seq.incrementAndGet(), event, data);
            keep(id, e);
            stream(id, event, data);
            wake(id);
        });
    }

    // ---------- mailbox ----------

    private void keep(long userId, Event e) {
        Deque<Stamped> box = mailboxes.computeIfAbsent(userId, k -> new ArrayDeque<>());
        long now = System.currentTimeMillis();
        synchronized (box) {
            box.addLast(new Stamped(e, now));
            while (box.size() > MAILBOX_SIZE || (!box.isEmpty() && now - box.peekFirst().at() > MAILBOX_MILLIS)) {
                box.removeFirst();
            }
        }
    }

    private List<Event> since(long userId, long after) {
        Deque<Stamped> box = mailboxes.get(userId);
        List<Event> out = new ArrayList<>();
        if (box != null) {
            synchronized (box) {
                for (Stamped s : box) {
                    if (s.event().seq() > after) {
                        out.add(s.event());
                    }
                }
            }
        }
        return out;
    }

    private void wake(long userId) {
        List<Waiter> list = waiters.get(userId);
        if (list == null) {
            return;
        }
        for (Waiter w : list) {
            List<Event> ready = since(userId, w.after());
            if (!ready.isEmpty()) {
                w.result().setResult(new Poll(ready.get(ready.size() - 1).seq(), ready));
                list.remove(w);
            }
        }
    }

    // ---------- SSE ----------

    private void stream(long userId, String event, Object data) {
        List<SseEmitter> list = streams.get(userId);
        if (list == null) {
            return;
        }
        for (SseEmitter emitter : list) {
            try {
                if (event == null) {
                    emitter.send(SseEmitter.event().comment("ping"));
                } else {
                    emitter.send(SseEmitter.event().name(event).data(data));
                }
            } catch (IOException | IllegalStateException e) {
                list.remove(emitter);
            }
        }
    }
}
