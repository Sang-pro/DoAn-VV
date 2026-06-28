import 'dart:convert';
import 'package:http/http.dart' as http;
import 'package:shared_preferences/shared_preferences.dart';
import '../models/book.dart';
import '../models/user.dart';
import '../models/borrow_record.dart';

class ApiService {
  // Thay đổi IP này thành IPv4 tĩnh của máy tính bạn (ví dụ: 192.168.1.x)
  // Nếu dùng máy ảo Android, dùng 10.0.2.2.
  // Nếu chạy máy thật, dùng IP LAN của máy tính: 192.168.1.18
  static const String baseUrl = 'https://fax-sterling-antelope.ngrok-free.dev/api';


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
      if (user.userCode != null) {
        await prefs.setString('userCode', user.userCode!);
      } else {
        await prefs.remove('userCode');
      }
      
      return user;
    }
    return null;
  }

  Future<Map<String, dynamic>> register({
    required String username,
    required String password,
    required String email,
    String? phoneNumber,
  }) async {
    try {
      final response = await http.post(
        Uri.parse('$baseUrl/auth/register'),
        headers: {'Content-Type': 'application/json'},
        body: jsonEncode({
          'username': username,
          'password': password,
          'email': email,
          'phoneNumber': phoneNumber ?? '',
        }),
      );

      final decoded = jsonDecode(utf8.decode(response.bodyBytes));
      if (response.statusCode == 200 || response.statusCode == 201) {
        return {
          'success': true,
          'message': decoded['message'] ?? 'Đăng ký tài khoản thành công!',
        };
      } else {
        return {
          'success': false,
          'message': decoded['message'] ?? 'Đăng ký tài khoản thất bại!',
        };
      }
    } catch (e) {
      return {
        'success': false,
        'message': 'Lỗi kết nối: ${e.toString()}',
      };
    }
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
      if (user.userCode != null) {
        await prefs.setString('userCode', user.userCode!);
      } else {
        await prefs.remove('userCode');
      }
      
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
      if (user.userCode != null) {
        await prefs.setString('userCode', user.userCode!);
      } else {
        await prefs.remove('userCode');
      }
      
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
      Uri.parse('$baseUrl/esl/find/$bookId'),
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

  // Borrow & Return API
  Future<Map<String, dynamic>> borrowBook(int bookId, String userCode) async {
    final headers = await _getHeaders();
    final response = await http.post(
      Uri.parse('$baseUrl/borrow/borrow'),
      headers: headers,
      body: jsonEncode({
        'bookId': bookId,
        'userCode': userCode,
      }),
    );

    final decoded = jsonDecode(utf8.decode(response.bodyBytes));
    if (response.statusCode == 200) {
      return decoded;
    } else {
      throw Exception(decoded['message'] ?? 'Lỗi khi thực hiện mượn sách');
    }
  }

  Future<Map<String, dynamic>> returnBook(int bookId, String userCode) async {
    final headers = await _getHeaders();
    final response = await http.post(
      Uri.parse('$baseUrl/borrow/return'),
      headers: headers,
      body: jsonEncode({
        'bookId': bookId,
        'userCode': userCode,
      }),
    );

    final decoded = jsonDecode(utf8.decode(response.bodyBytes));
    if (response.statusCode == 200) {
      return decoded;
    } else {
      throw Exception(decoded['message'] ?? 'Lỗi khi thực hiện trả sách');
    }
  }

  Future<List<BorrowRecord>> getBorrowHistory(String userCode) async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl/borrow/user/$userCode'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
      return data.map((json) => BorrowRecord.fromJson(json)).toList();
    } else {
      final decoded = jsonDecode(utf8.decode(response.bodyBytes));
      throw Exception(decoded['message'] ?? 'Lỗi khi tải lịch sử mượn sách');
    }
  }

  // RFID API
  Future<List<RfidTag>> getRfidTags() async {
    final headers = await _getHeaders();
    final response = await http.get(
      Uri.parse('$baseUrl/rfid'),
      headers: headers,
    );

    if (response.statusCode == 200) {
      final List<dynamic> data = jsonDecode(utf8.decode(response.bodyBytes));
      return data.map((json) => RfidTag.fromJson(json)).toList();
    }
    throw Exception('Không thể tải danh sách mã RFID (${response.statusCode})');
  }

  Future<RfidTag> registerRfidTag(String epc, int bookId) async {
    final headers = await _getHeaders();
    final response = await http.post(
      Uri.parse('$baseUrl/rfid/register'),
      headers: headers,
      body: jsonEncode({
        'epc': epc,
        'bookId': bookId,
      }),
    );

    final decoded = jsonDecode(utf8.decode(response.bodyBytes));
    if (response.statusCode == 200) {
      return RfidTag.fromJson(decoded);
    }
    throw Exception(decoded['message'] ?? 'Đăng ký mã RFID thất bại');
  }

  Future<void> deleteRfidTag(int id) async {
    final headers = await _getHeaders();
    final response = await http.delete(
      Uri.parse('$baseUrl/rfid/$id'),
      headers: headers,
    );

    if (response.statusCode != 200) {
      throw Exception('Xóa mã RFID thất bại');
    }
  }
}
