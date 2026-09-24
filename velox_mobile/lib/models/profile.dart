/// Authenticated user profile (snake_case keys come from the backend).
class Profile {
  final int id;
  final String fullName;
  final String email;
  final String phone;
  final String governorate;
  final String role;
  final bool verified;

  const Profile({
    required this.id,
    required this.fullName,
    required this.email,
    required this.phone,
    required this.governorate,
    this.role = 'CUSTOMER',
    this.verified = true,
  });

  factory Profile.fromJson(Map<String, dynamic> j) => Profile(
        id: (j['id'] as num?)?.toInt() ?? 0,
        fullName: (j['full_name'] ?? j['fullName'] ?? j['name'] ?? '')
            .toString(),
        email: (j['email'] ?? '').toString(),
        phone: (j['phone_number'] ?? j['phone'] ?? '').toString(),
        governorate: (j['governorate'] ?? 'CAIRO').toString(),
        role: (j['role'] ?? 'CUSTOMER').toString(),
        verified: (j['is_verified'] ?? j['verified'] ?? true) == true ||
            (j['is_verified'] ?? j['verified'] ?? 1).toString() == '1',
      );

  Profile copyWith({String? fullName, String? phone, String? governorate}) =>
      Profile(
        id: id,
        fullName: fullName ?? this.fullName,
        email: email,
        phone: phone ?? this.phone,
        governorate: governorate ?? this.governorate,
        role: role,
        verified: verified,
      );
}