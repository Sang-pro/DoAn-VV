class User {
  final String username;
  final String token;
  final List<String> roles;

  User({
    required this.username,
    required this.token,
    required this.roles,
  });

  factory User.fromJson(Map<String, dynamic> json) {
    return User(
      username: json['username'] ?? '',
      token: json['accessToken'] ?? json['token'] ?? '',
      roles: List<String>.from(json['roles'] ?? []),
    );
  }
}
