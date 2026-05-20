import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/book.dart';
import '../models/user.dart';

class ApiService {
  // Thay đổi IP này thành IPv4 tĩnh của máy tính bạn (ví dụ: 192.168.1.x)
  // Nếu dùng máy ảo Android, dùng 10.0.2.2.
  // Nếu chạy máy thật, dùng IP LAN của máy tính: 192.168.1.18
  static const String baseUrl = 'http://192.168.1.18:8080/api';


  Future<Map<String, String>> _getHeaders() async {
    final prefs = await SharedPreferences.getInstance();
    final token = prefs.getString('token');
    return {
      'Content-Type': 'application/json',
      if (token != null) 'Authorization': 'Bearer $token',
    };
  }

  // Auth API
  Future<User?> login(String username, String password) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/login'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'username': username, 'password': password}),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      final user = User.fromJson(data);
      
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('token', user.token);
      await prefs.setString('username', user.username);
      await prefs.setStringList('roles', user.roles);
      
      return user;
    }
    return null;
  }

  // OAuth API
  Future<User?> loginWithGoogle(String idToken) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/google'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'token': idToken}),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      final user = User.fromJson(data);
      
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('token', user.token);
      await prefs.setString('username', user.username);
      await prefs.setStringList('roles', user.roles);
      
      return user;
    }
    return null;
  }

  Future<User?> loginWithFacebook(String accessToken) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/facebook'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'token': accessToken}),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(response.body);
      final user = User.fromJson(data);
      
      final prefs = await SharedPreferences.getInstance();
      await prefs.setString('token', user.token);
      await prefs.setString('username', user.username);
      await prefs.setStringList('roles', user.roles);
      
      return user;
    }
    return null;
  }

  // Books API
  Future<List<Book>> getBooks() async {
    final headers = await _getHeaders();
    final response = await http.get(Uri.parse('$baseUrl/books'), headers: headers);

    if (response.statusCode == 200) {
      final dynamic decoded = jsonDecode(utf8.decode(response.bodyBytes));
      List<dynamic> data;
      if (decoded is Map<String, dynamic> && decoded.containsKey('content')) {
        data = decoded['content'] as List<dynamic>;
      } else if (decoded is List<dynamic>) {
        data = decoded;
      } else {
        throw Exception('Định dạng dữ liệu sách không hợp lệ');
      }
      return data.map((json) => Book.fromJson(json)).toList();
    }
    throw Exception('Failed to load books');
  }

  Future<Book> updateBook(int id, Map<String, dynamic> bookData) async {
    final headers = await _getHeaders();
    final response = await http.put(
      Uri.parse('$baseUrl/books/$id'),
      headers: headers,
      body: jsonEncode(bookData),
    );

    if (response.statusCode == 200) {
      return Book.fromJson(jsonDecode(utf8.decode(response.bodyBytes)));
    }
    throw Exception('Failed to update book');
  }

  // ESL Tag Pick To Light
  Future<void> pickToLight(int bookId) async {
    final headers = await _getHeaders();
    final response = await http.post(
      Uri.parse('$baseUrl/esl-tags/pick-to-light/$bookId'),
      headers: headers,
    );

    if (response.statusCode != 200) {
      throw Exception('Failed to trigger Pick-to-light');
    }
  }

  // AI Chat API
  Future<String> chatWithAI(List<Map<String, String>> messages) async {
    final headers = await _getHeaders();
    final response = await http.post(
      Uri.parse('$baseUrl/ollama/chat'),
      headers: headers,
      body: jsonEncode({
        'model': 'deepseek-r1:8b', // Hoặc mô hình mặc định
        'messages': messages,
        'streaming': false,
      }),
    );

    if (response.statusCode == 200) {
      final data = jsonDecode(utf8.decode(response.bodyBytes));
      if (data['message'] != null && data['message']['content'] != null) {
        return data['message']['content'];
      }
    }
    throw Exception('Failed to communicate with AI');
  }

  // ESL Tags API
  Future<List<EslTag>> getEslTags() async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl/esl'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      try {
        final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
        return data.map((json) => EslTag.fromJson(json)).toList();
      } catch (e) {
        throw Exception('Lỗi parse JSON thẻ ESL: $e');
      }
    }
    throw Exception('Lỗi kết nối API (${response.statusCode}): ${response.body}');
  }


  // Quên mật khẩu API
  Future<String> requestPasswordReset(String email) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/forgot-password'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'email': email}),
    );
    final data = jsonDecode(response.body);
    if (response.statusCode == 200 && data['success'] == true) {
      return data['message'] ?? 'OTP đã được gửi';
    }
    throw Exception(data['message'] ?? 'Lỗi khi gửi yêu cầu khôi phục');
  }

  Future<String> verifyResetOtp(String email, String otp) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/verify-otp'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'email': email, 'otp': otp}),
    );
    final data = jsonDecode(response.body);
    if (response.statusCode == 200 && data['success'] == true) {
      return data['token'] ?? ''; // Trả về resetToken
    }
    throw Exception(data['message'] ?? 'Mã OTP không hợp lệ');
  }

  Future<String> resetPassword(String email, String resetToken, String newPassword) async {
    final response = await http.post(
      Uri.parse('$baseUrl/auth/reset-password'),
      headers: {'Content-Type': 'application/json'},
      body: jsonEncode({'email': email, 'resetToken': resetToken, 'newPassword': newPassword}),
    );
    final data = jsonDecode(response.body);
    if (response.statusCode == 200 && data['success'] == true) {
      return data['message'] ?? 'Đổi mật khẩu thành công';
    }
    throw Exception(data['message'] ?? 'Lỗi khi đặt lại mật khẩu');
  }
}
