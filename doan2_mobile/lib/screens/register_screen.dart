import 'package:flutter/material.dart';
import '../services/api_service.dart';

class RegisterScreen extends StatefulWidget {
  const RegisterScreen({super.key});

  @override
  _RegisterScreenState createState() => _RegisterScreenState();
}

class _RegisterScreenState extends State<RegisterScreen> {
  final ApiService _apiService = ApiService();
  final _formKey = GlobalKey<FormState>();

  final _usernameController = TextEditingController();
  final _emailController = TextEditingController();
  final _phoneController = TextEditingController();
  final _passwordController = TextEditingController();
  final _confirmPasswordController = TextEditingController();

  bool _isLoading = false;
  bool _isPasswordVisible = false;
  bool _isConfirmPasswordVisible = false;

  void _handleRegister() async {
    if (!_formKey.currentState!.validate()) return;

    if (_passwordController.text != _confirmPasswordController.text) {
      _showSnackbar('Mật khẩu xác nhận không khớp!', isError: true);
      return;
    }

    setState(() => _isLoading = true);

    final result = await _apiService.register(
      username: _usernameController.text.trim(),
      password: _passwordController.text,
      email: _emailController.text.trim(),
      phoneNumber: _phoneController.text.trim(),
    );

    setState(() => _isLoading = false);

    if (result['success'] == true) {
      _showSnackbar(result['message'] ?? 'Đăng ký tài khoản thành công!', isError: false);
      // Wait for snackbar to show, then go back to login screen
      Future.delayed(const Duration(seconds: 1, milliseconds: 500), () {
        if (mounted) {
          Navigator.of(context).pop();
        }
      });
    } else {
      _showSnackbar(result['message'] ?? 'Đăng ký tài khoản thất bại!', isError: true);
    }
  }

  void _showSnackbar(String message, {required bool isError}) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(
        content: Text(message),
        backgroundColor: isError ? Colors.red[600] : Colors.green[600],
        behavior: SnackBarBehavior.floating,
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.transparent,
        elevation: 0,
        foregroundColor: Colors.black87,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          child: Padding(
            padding: const EdgeInsets.symmetric(horizontal: 24.0),
            child: Form(
              key: _formKey,
              child: Column(
                crossAxisAlignment: CrossAxisAlignment.stretch,
                children: [
                  // Header section
                  Center(
                    child: Container(
                      padding: const EdgeInsets.all(16),
                      decoration: BoxDecoration(
                        color: Colors.indigo[50],
                        shape: BoxShape.circle,
                      ),
                      child: Icon(Icons.person_add_alt_1_rounded, size: 50, color: Colors.indigo[600]),
                    ),
                  ),
                  const SizedBox(height: 20),
                  const Text(
                    'Đăng Ký Tài Khoản',
                    textAlign: TextAlign.center,
                    style: TextStyle(
                      fontSize: 26,
                      fontWeight: FontWeight.bold,
                      letterSpacing: -0.5,
                    ),
                  ),
                  const SizedBox(height: 8),
                  Text(
                    'Tạo tài khoản để bắt đầu trải nghiệm Hệ thống quản lý thư viện',
                    textAlign: TextAlign.center,
                    style: TextStyle(fontSize: 14, color: Colors.grey[500]),
                  ),
                  const SizedBox(height: 32),

                  // Username Input
                  _buildTextFormField(
                    controller: _usernameController,
                    label: 'Tên đăng nhập',
                    icon: Icons.person_outline,
                    validator: (val) {
                      if (val == null || val.trim().isEmpty) return 'Tên đăng nhập là bắt buộc';
                      if (val.trim().length < 3) return 'Tên đăng nhập phải có ít nhất 3 ký tự';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),

                  // Email Input
                  _buildTextFormField(
                    controller: _emailController,
                    label: 'Địa chỉ Email',
                    icon: Icons.mail_outline,
                    keyboardType: TextInputType.emailAddress,
                    validator: (val) {
                      if (val == null || val.trim().isEmpty) return 'Email là bắt buộc';
                      final emailReg = RegExp(r'^[\w-\.]+@([\w-]+\.)+[\w-]{2,4}$');
                      if (!emailReg.hasMatch(val.trim())) return 'Email không đúng định dạng';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),

                  // Phone Input
                  _buildTextFormField(
                    controller: _phoneController,
                    label: 'Số điện thoại (Tùy chọn)',
                    icon: Icons.phone_android_outlined,
                    keyboardType: TextInputType.phone,
                    validator: (val) {
                      if (val != null && val.trim().isNotEmpty) {
                        final phoneReg = RegExp(r'^\+?[0-9]{10,15}$');
                        if (!phoneReg.hasMatch(val.trim())) return 'Số điện thoại không hợp lệ';
                      }
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),

                  // Password Input
                  _buildTextFormField(
                    controller: _passwordController,
                    label: 'Mật khẩu',
                    icon: Icons.lock_outline,
                    isPassword: true,
                    isPasswordVisible: _isPasswordVisible,
                    togglePasswordVisibility: () => setState(() => _isPasswordVisible = !_isPasswordVisible),
                    validator: (val) {
                      if (val == null || val.isEmpty) return 'Mật khẩu là bắt buộc';
                      if (val.length < 8) return 'Mật khẩu phải chứa ít nhất 8 ký tự';
                      return null;
                    },
                  ),
                  const SizedBox(height: 16),

                  // Confirm Password Input
                  _buildTextFormField(
                    controller: _confirmPasswordController,
                    label: 'Xác nhận mật khẩu',
                    icon: Icons.lock_reset_outlined,
                    isPassword: true,
                    isPasswordVisible: _isConfirmPasswordVisible,
                    togglePasswordVisibility: () => setState(() => _isConfirmPasswordVisible = !_isConfirmPasswordVisible),
                    validator: (val) {
                      if (val == null || val.isEmpty) return 'Vui lòng xác nhận mật khẩu';
                      return null;
                    },
                  ),
                  const SizedBox(height: 32),

                  // Register Button
                  SizedBox(
                    height: 56,
                    child: ElevatedButton(
                      onPressed: _isLoading ? null : _handleRegister,
                      style: ElevatedButton.styleFrom(
                        backgroundColor: Colors.indigo[600],
                        foregroundColor: Colors.white,
                        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                        elevation: 4,
                        shadowColor: Colors.indigo.withOpacity(0.4),
                      ),
                      child: _isLoading
                          ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                          : const Text(
                              'ĐĂNG KÝ NGAY',
                              style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, letterSpacing: 1.2),
                            ),
                    ),
                  ),
                  const SizedBox(height: 24),

                  // Link back to Login
                  Row(
                    mainAxisAlignment: MainAxisAlignment.center,
                    children: [
                      Text('Đã có tài khoản? ', style: TextStyle(color: Colors.grey[600])),
                      GestureDetector(
                        onTap: () => Navigator.of(context).pop(),
                        child: Text(
                          'Đăng nhập',
                          style: TextStyle(
                            color: Colors.indigo[600],
                            fontWeight: FontWeight.bold,
                          ),
                        ),
                      ),
                    ],
                  ),
                  const SizedBox(height: 32),
                ],
              ),
            ),
          ),
        ),
      ),
    );
  }

  Widget _buildTextFormField({
    required TextEditingController controller,
    required String label,
    required IconData icon,
    bool isPassword = false,
    bool isPasswordVisible = false,
    VoidCallback? togglePasswordVisibility,
    TextInputType keyboardType = TextInputType.text,
    String? Function(String?)? validator,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.grey[50],
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey[200]!),
      ),
      child: TextFormField(
        controller: controller,
        obscureText: isPassword && !isPasswordVisible,
        keyboardType: keyboardType,
        validator: validator,
        style: const TextStyle(fontSize: 16),
        decoration: InputDecoration(
          labelText: label,
          labelStyle: TextStyle(color: Colors.grey[500], fontSize: 14),
          prefixIcon: Icon(icon, color: Colors.indigo[400]),
          suffixIcon: isPassword
              ? IconButton(
                  icon: Icon(
                    isPasswordVisible ? Icons.visibility : Icons.visibility_off,
                    color: Colors.grey[500],
                  ),
                  onPressed: togglePasswordVisibility,
                )
              : null,
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
          errorStyle: const TextStyle(height: 0.8, fontSize: 12),
        ),
      ),
    );
  }
}
