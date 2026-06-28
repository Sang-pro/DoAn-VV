import 'package:flutter/material.dart';
import 'package:flutter/services.dart';
import 'package:provider/provider.dart';
import '../providers/auth_provider.dart';
import 'login_screen.dart';
import 'esl_management_screen.dart';
import 'rfid_management_screen.dart';
import 'borrow_history_screen.dart';

class ProfileScreen extends StatelessWidget {
  const ProfileScreen({super.key});

  @override
  Widget build(BuildContext context) {
    final authProvider = Provider.of<AuthProvider>(context);
    final user = authProvider.user;

    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text('Tài khoản cá nhân', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.indigo[600],
        foregroundColor: Colors.white,
        elevation: 0,
      ),
      body: SingleChildScrollView(
        child: Column(
          children: [
            // Header Profile
            Container(
              width: double.infinity,
              padding: const EdgeInsets.only(top: 32, bottom: 40, left: 16, right: 16),
              decoration: BoxDecoration(
                color: Colors.indigo[600],
                borderRadius: const BorderRadius.only(
                  bottomLeft: Radius.circular(32),
                  bottomRight: Radius.circular(32),
                ),
              ),
              child: Column(
                children: [
                  CircleAvatar(
                    radius: 50,
                    backgroundColor: Colors.white,
                    child: Text(
                      user?.username.substring(0, 1).toUpperCase() ?? 'U',
                      style: TextStyle(fontSize: 40, fontWeight: FontWeight.bold, color: Colors.indigo[600]),
                    ),
                  ),
                  const SizedBox(height: 16),
                  Text(
                    user?.username ?? 'Chưa đăng nhập',
                    style: const TextStyle(fontSize: 24, fontWeight: FontWeight.bold, color: Colors.white),
                  ),
                  if (user?.userCode != null) ...[
                    const SizedBox(height: 6),
                    Text(
                      'Mã số: ${user!.userCode}',
                      style: const TextStyle(fontSize: 16, color: Colors.white70, fontWeight: FontWeight.w500, letterSpacing: 1.1),
                    ),
                  ],
                  const SizedBox(height: 8),
                  Container(
                    padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
                    decoration: BoxDecoration(
                      color: Colors.indigo[400],
                      borderRadius: BorderRadius.circular(20),
                    ),
                    child: Text(
                      user?.roles.join(', ') ?? 'USER',
                      style: const TextStyle(color: Colors.white, fontSize: 12, fontWeight: FontWeight.w600),
                    ),
                  ),
                ],
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Menu Items
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: Card(
                elevation: 2,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                child: Column(
                  children: [
                    if (user?.userCode != null) ...[
                      _buildInfoItem(
                        icon: Icons.badge_outlined,
                        title: 'Mã số người dùng (User Code)',
                        value: user!.userCode!,
                        onCopy: () {
                          Clipboard.setData(ClipboardData(text: user.userCode!));
                          ScaffoldMessenger.of(context).showSnackBar(
                            const SnackBar(
                              content: Text('Đã sao chép mã số người dùng!'),
                              backgroundColor: Colors.indigo,
                              duration: Duration(seconds: 2),
                            ),
                          );
                        },
                      ),
                      const Divider(height: 1),
                    ],
                    _buildMenuItem(
                      icon: Icons.history,
                      title: 'Lịch sử mượn sách',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => const BorrowHistoryScreen(initialTabIndex: 1),
                          ),
                        );
                      },
                    ),
                    const Divider(height: 1),
                    _buildMenuItem(
                      icon: Icons.bookmark_border,
                      title: 'Sách đang mượn',
                      onTap: () {
                        Navigator.push(
                          context,
                          MaterialPageRoute(
                            builder: (context) => const BorrowHistoryScreen(initialTabIndex: 0),
                          ),
                        );
                      },
                    ),
                    const Divider(height: 1),
                    _buildMenuItem(
                      icon: Icons.lock_outline,
                      title: 'Đổi mật khẩu',
                      onTap: () {
                        // TODO: Implement navigation
                      },
                    ),
                    if (authProvider.hasRole('ROLE_ADMIN')) ...[
                      const Divider(height: 1),
                      _buildMenuItem(
                        icon: Icons.important_devices,
                        title: 'Quản lý Thẻ điện tử (ESL)',
                        onTap: () {
                          Navigator.of(context).push(
                            MaterialPageRoute(builder: (_) => const EslManagementScreen()),
                          );
                        },
                      ),
                    ],
                    if (authProvider.hasRole('ROLE_ADMIN') || authProvider.hasRole('ROLE_LIBRARIAN')) ...[
                      const Divider(height: 1),
                      _buildMenuItem(
                        icon: Icons.label_outline,
                        title: 'Quản lý mã dán RFID',
                        onTap: () {
                          Navigator.of(context).push(
                            MaterialPageRoute(builder: (_) => const RfidManagementScreen()),
                          );
                        },
                      ),
                    ],
                  ],
                ),
              ),
            ),
            
            const SizedBox(height: 24),
            
            // Logout Button
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              child: SizedBox(
                width: double.infinity,
                height: 50,
                child: ElevatedButton.icon(
                  onPressed: () {
                    _showLogoutDialog(context, authProvider);
                  },
                  icon: const Icon(Icons.logout),
                  label: const Text('Đăng xuất', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.red[50],
                    foregroundColor: Colors.red[600],
                    elevation: 0,
                    shape: RoundedRectangleBorder(
                      borderRadius: BorderRadius.circular(16),
                      side: BorderSide(color: Colors.red[200]!),
                    ),
                  ),
                ),
              ),
            ),
            const SizedBox(height: 32),
          ],
        ),
      ),
    );
  }

  Widget _buildMenuItem({required IconData icon, required String title, required VoidCallback onTap}) {
    return ListTile(
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: Colors.indigo[50],
          borderRadius: BorderRadius.circular(8),
        ),
        child: Icon(icon, color: Colors.indigo[600]),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w500)),
      trailing: const Icon(Icons.chevron_right, color: Colors.grey),
      onTap: onTap,
    );
  }

  Widget _buildInfoItem({
    required IconData icon,
    required String title,
    required String value,
    required VoidCallback onCopy,
  }) {
    return ListTile(
      leading: Container(
        padding: const EdgeInsets.all(8),
        decoration: BoxDecoration(
          color: Colors.indigo[50],
          borderRadius: BorderRadius.circular(8),
        ),
        child: Icon(icon, color: Colors.indigo[600]),
      ),
      title: Text(title, style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 13, color: Colors.grey)),
      subtitle: Text(
        value,
        style: const TextStyle(
          fontWeight: FontWeight.bold,
          fontSize: 18,
          color: Colors.black87,
          letterSpacing: 1.1,
        ),
      ),
      trailing: IconButton(
        icon: const Icon(Icons.copy, size: 20, color: Colors.indigo),
        onPressed: onCopy,
        tooltip: 'Sao chép',
      ),
    );
  }

  void _showLogoutDialog(BuildContext context, AuthProvider authProvider) {
    showDialog(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Xác nhận đăng xuất'),
        content: const Text('Bạn có chắc chắn muốn đăng xuất khỏi ứng dụng?'),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () {
              authProvider.logout();
              Navigator.of(context).pushAndRemoveUntil(
                MaterialPageRoute(builder: (_) => const LoginScreen()),
                (route) => false,
              );
            },
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.red[600],
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            child: const Text('Đăng xuất'),
          ),
        ],
      ),
    );
  }
}
