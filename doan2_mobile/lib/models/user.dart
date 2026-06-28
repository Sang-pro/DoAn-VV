class User {
  final String username;
  final String token;
  final List<String> roles;
  final String? userCode;

  User({
    required this.username,
    required this.token,
    required this.roles,
    this.userCode,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      username: json['username'] ?? '',
      token: json['accessToken'] ?? json['token'] ?? '',
      roles: List<String>.from(json['roles'] ?? []),
      userCode: json['userCode'],
    );
  }
}
