import 'package:flutter/material.dart';
import '../services/api_service.dart';

enum ForgotPasswordStep { email, otp, newPassword }

class ForgotPasswordScreen extends StatefulWidget {
  const ForgotPasswordScreen({super.key});

  @override
  _ForgotPasswordScreenState createState() => _ForgotPasswordScreenState();
}

class _ForgotPasswordScreenState extends State<ForgotPasswordScreen> {
  final ApiService _apiService = ApiService();
  
  ForgotPasswordStep _currentStep = ForgotPasswordStep.email;
  bool _isLoading = false;
  bool _isPasswordVisible = false;
  
  String _email = '';
  String _resetToken = '';
  
  final _emailController = TextEditingController();
  final _otpController = TextEditingController();
  final _passwordController = TextEditingController();

  void _showError(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.red[600], behavior: SnackBarBehavior.floating),
    );
  }

  void _showSuccess(String message) {
    ScaffoldMessenger.of(context).showSnackBar(
      SnackBar(content: Text(message), backgroundColor: Colors.green[600], behavior: SnackBarBehavior.floating),
    );
  }

  Future<void> _submitEmail() async {
    final email = _emailController.text.trim();
    if (email.isEmpty || !email.contains('@')) {
      _showError('Vui lòng nhập email hợp lệ');
      return;
    }

    setState(() => _isLoading = true);
    try {
      final msg = await _apiService.requestPasswordReset(email);
      _email = email;
      _showSuccess(msg);
      setState(() => _currentStep = ForgotPasswordStep.otp);
    } catch (e) {
      _showError(e.toString().replaceAll('Exception: ', ''));
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _submitOtp() async {
    final otp = _otpController.text.trim();
    if (otp.isEmpty) {
      _showError('Vui lòng nhập mã OTP');
      return;
    }

    setState(() => _isLoading = true);
    try {
      final token = await _apiService.verifyResetOtp(_email, otp);
      _resetToken = token;
      setState(() => _currentStep = ForgotPasswordStep.newPassword);
    } catch (e) {
      _showError(e.toString().replaceAll('Exception: ', ''));
    } finally {
      setState(() => _isLoading = false);
    }
  }

  Future<void> _submitNewPassword() async {
    final newPassword = _passwordController.text;
    if (newPassword.length < 6) {
      _showError('Mật khẩu phải có ít nhất 6 ký tự');
      return;
    }

    setState(() => _isLoading = true);
    try {
      final msg = await _apiService.resetPassword(_email, _resetToken, newPassword);
      _showSuccess(msg);
      Navigator.of(context).pop(); // Quay lại trang đăng nhập
    } catch (e) {
      _showError(e.toString().replaceAll('Exception: ', ''));
    } finally {
      setState(() => _isLoading = false);
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      appBar: AppBar(
        backgroundColor: Colors.white,
        foregroundColor: Colors.indigo[800],
        elevation: 0,
      ),
      body: SafeArea(
        child: SingleChildScrollView(
          padding: const EdgeInsets.symmetric(horizontal: 32.0, vertical: 24.0),
          child: Column(
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Icon/Illustration
              Container(
                height: 120,
                width: 120,
                decoration: BoxDecoration(
                  color: Colors.indigo[50],
                  shape: BoxShape.circle,
                ),
                child: Icon(
                  _currentStep == ForgotPasswordStep.email
                      ? Icons.mark_email_read_outlined
                      : _currentStep == ForgotPasswordStep.otp
                          ? Icons.domain_verification_outlined
                          : Icons.lock_reset_outlined,
                  size: 60,
                  color: Colors.indigo[600],
                ),
              ),
              const SizedBox(height: 32),
              
              // Tiêu đề
              Text(
                _currentStep == ForgotPasswordStep.email 
                    ? 'Quên mật khẩu?' 
                    : _currentStep == ForgotPasswordStep.otp 
                        ? 'Xác thực OTP' 
                        : 'Mật khẩu mới',
                style: TextStyle(
                  fontSize: 28, 
                  fontWeight: FontWeight.w800,
                  color: Colors.indigo[900],
                  letterSpacing: -0.5,
                ),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 12),
              
              // Mô tả
              Text(
                _currentStep == ForgotPasswordStep.email 
                    ? 'Đừng lo lắng! Hãy nhập email được liên kết với tài khoản của bạn để nhận mã khôi phục.' 
                    : _currentStep == ForgotPasswordStep.otp 
                        ? 'Nhập mã 6 số vừa được gửi đến email\n$_email' 
                        : 'Vui lòng tạo một mật khẩu mới mạnh mẽ cho tài khoản của bạn.',
                style: TextStyle(fontSize: 15, color: Colors.grey[600], height: 1.5),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 48),
              
              // Trường nhập liệu
              if (_currentStep == ForgotPasswordStep.email)
                _buildTextField(
                  controller: _emailController,
                  label: 'Địa chỉ Email',
                  icon: Icons.email_outlined,
                  keyboardType: TextInputType.emailAddress,
                ),
                
              if (_currentStep == ForgotPasswordStep.otp)
                _buildTextField(
                  controller: _otpController,
                  label: 'Mã OTP (6 chữ số)',
                  icon: Icons.password,
                  keyboardType: TextInputType.number,
                ),
                
              if (_currentStep == ForgotPasswordStep.newPassword)
                _buildTextField(
                  controller: _passwordController,
                  label: 'Mật khẩu mới',
                  icon: Icons.lock_outline,
                  isPassword: true,
                ),
                
              const SizedBox(height: 32),
              
              // Nút bấm
              SizedBox(
                height: 56,
                child: ElevatedButton(
                  onPressed: _isLoading 
                      ? null 
                      : (_currentStep == ForgotPasswordStep.email 
                          ? _submitEmail 
                          : _currentStep == ForgotPasswordStep.otp 
                              ? _submitOtp 
                              : _submitNewPassword),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.indigo[600],
                    foregroundColor: Colors.white,
                    shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    elevation: 4,
                    shadowColor: Colors.indigo.withOpacity(0.4),
                  ),
                  child: _isLoading 
                      ? const SizedBox(width: 24, height: 24, child: CircularProgressIndicator(color: Colors.white, strokeWidth: 2.5))
                      : Text(
                          _currentStep == ForgotPasswordStep.email 
                              ? 'GỬI MÃ OTP' 
                              : _currentStep == ForgotPasswordStep.otp 
                                  ? 'XÁC NHẬN MÃ' 
                                  : 'ĐẶT LẠI MẬT KHẨU',
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold, letterSpacing: 1.2),
                        ),
                ),
              ),
              
              const SizedBox(height: 24),
              if (_currentStep == ForgotPasswordStep.email)
                Row(
                  mainAxisAlignment: MainAxisAlignment.center,
                  children: [
                    Text('Bạn đã nhớ ra mật khẩu? ', style: TextStyle(color: Colors.grey[600])),
                    GestureDetector(
                      onTap: () => Navigator.of(context).pop(),
                      child: Text(
                        'Đăng nhập ngay',
                        style: TextStyle(color: Colors.indigo[600], fontWeight: FontWeight.bold),
                      ),
                    ),
                  ],
                ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildTextField({
    required TextEditingController controller,
    required String label,
    required IconData icon,
    bool isPassword = false,
    TextInputType keyboardType = TextInputType.text,
  }) {
    return Container(
      decoration: BoxDecoration(
        color: Colors.grey[50],
        borderRadius: BorderRadius.circular(16),
        border: Border.all(color: Colors.grey[200]!),
      ),
      child: TextField(
        controller: controller,
        obscureText: isPassword && !_isPasswordVisible,
        keyboardType: keyboardType,
        style: const TextStyle(fontSize: 16),
        decoration: InputDecoration(
          labelText: label,
          labelStyle: TextStyle(color: Colors.grey[500]),
          prefixIcon: Icon(icon, color: Colors.indigo[400]),
          suffixIcon: isPassword
              ? IconButton(
                  icon: Icon(
                    _isPasswordVisible ? Icons.visibility : Icons.visibility_off,
                    color: Colors.grey[500],
                  ),
                  onPressed: () {
                    setState(() {
                      _isPasswordVisible = !_isPasswordVisible;
                    });
                  },
                )
              : null,
          border: InputBorder.none,
          contentPadding: const EdgeInsets.symmetric(horizontal: 20, vertical: 16),
        ),
      ),
    );
  }
}
