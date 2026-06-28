import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import '../models/book.dart';
import '../services/api_service.dart';
import '../providers/auth_provider.dart';

class BookDetailScreen extends StatefulWidget {
  final Book book;

  const BookDetailScreen({super.key, required this.book});

  @override
  _BookDetailScreenState createState() => _BookDetailScreenState();
}

class _BookDetailScreenState extends State<BookDetailScreen> {
  final ApiService _apiService = ApiService();
  bool _isLighting = false;
  late int _availableCopies;
  bool _isBorrowing = false;

  @override
  void initState() {
    super.initState();
    _availableCopies = widget.book.availableCopies;
  }

  void _borrowBook() async {
    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final user = authProvider.user;
    
    if (user == null || user.userCode == null || user.userCode!.isEmpty) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Không tìm thấy mã số độc giả của cậu. Vui lòng liên hệ thủ thư.'),
          backgroundColor: Colors.red,
        ),
      );
      return;
    }

    setState(() {
      _isBorrowing = true;
    });

    try {
      await _apiService.borrowBook(widget.book.id!, user.userCode!);
      
      setState(() {
        _availableCopies = _availableCopies - 1;
      });

      if (!mounted) return;
      showDialog(
        context: context,
        barrierDismissible: false,
        builder: (BuildContext context) {
          return AlertDialog(
            shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(20)),
            title: const Row(
              children: [
                Icon(Icons.check_circle, color: Colors.green, size: 28),
                SizedBox(width: 10),
                Text('Thành Công', style: TextStyle(fontWeight: FontWeight.bold)),
              ],
            ),
            content: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Text('Đăng ký mượn cuốn sách "${widget.book.title}" thành công!', style: const TextStyle(fontSize: 15)),
                const SizedBox(height: 12),
                Text('Hạn trả sách của cậu là 14 ngày kể từ hôm nay.', style: TextStyle(color: Colors.grey[600], fontSize: 13)),
                const SizedBox(height: 6),
                const Text('Hệ thống đã tự động cập nhật số lượng trên nhãn ESL tại kệ.', style: TextStyle(color: Colors.indigo, fontWeight: FontWeight.w500, fontSize: 13)),
              ],
            ),
            actions: [
              TextButton(
                onPressed: () {
                  Navigator.of(context).pop();
                },
                child: Text('Đồng ý', style: TextStyle(color: Colors.indigo[600], fontWeight: FontWeight.bold, fontSize: 15)),
              ),
            ],
          );
        },
      );
    } catch (e) {
      if (!mounted) return;
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Lỗi: ${e.toString().replaceAll('Exception: ', '')}'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      if (mounted) {
        setState(() {
          _isBorrowing = false;
        });
      }
    }
  }

  void _triggerPickToLight() async {
    if (widget.book.id == null) return;
    
    setState(() {
      _isLighting = true;
    });

    try {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Đang gửi tín hiệu đến kệ sách...'),
          duration: Duration(seconds: 1),
        ),
      );
      
      await _apiService.pickToLight(widget.book.id!);
      
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Row(
            children: [
              Icon(Icons.check_circle, color: Colors.white),
              SizedBox(width: 8),
              Text('Đã bật đèn! Hãy tìm đèn nhấp nháy trên kệ.'),
            ],
          ),
          backgroundColor: Colors.green,
          duration: Duration(seconds: 4),
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Lỗi: Kệ sách không phản hồi. Vui lòng thử lại sau.'),
          backgroundColor: Colors.red,
        ),
      );
    } finally {
      setState(() {
        _isLighting = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.white,
      body: CustomScrollView(
        slivers: [
          _buildSliverAppBar(),
          SliverToBoxAdapter(
            child: _buildBookInfo(),
          ),
        ],
      ),
      bottomNavigationBar: _buildBottomBar(),
    );
  }

  Widget _buildSliverAppBar() {
    return SliverAppBar(
      expandedHeight: 350.0,
      pinned: true,
      backgroundColor: Colors.indigo[600],
      foregroundColor: Colors.white,
      flexibleSpace: FlexibleSpaceBar(
        background: Stack(
          fit: StackFit.expand,
          children: [
            // Bìa sách mờ làm nền
            if (widget.book.coverImageUrl != null && widget.book.coverImageUrl!.isNotEmpty)
              Image.network(
                widget.book.coverImageUrl!,
                fit: BoxFit.cover,
                color: Colors.black.withOpacity(0.5),
                colorBlendMode: BlendMode.darken,
                errorBuilder: (_, __, ___) => Container(color: Colors.indigo[800]),
              )
            else
              Container(color: Colors.indigo[800]),
            
            // Bìa sách chính
            Center(
              child: Padding(
                padding: const EdgeInsets.only(top: 60, bottom: 20),
                child: Container(
                  width: 150,
                  height: 220,
                  decoration: BoxDecoration(
                    borderRadius: BorderRadius.circular(12),
                    boxShadow: [
                      BoxShadow(
                        color: Colors.black.withOpacity(0.3),
                        blurRadius: 20,
                        offset: const Offset(0, 10),
                      ),
                    ],
                  ),
                  clipBehavior: Clip.antiAlias,
                  child: widget.book.coverImageUrl != null && widget.book.coverImageUrl!.isNotEmpty
                      ? Image.network(
                          widget.book.coverImageUrl!,
                          fit: BoxFit.cover,
                          errorBuilder: (_, __, ___) => Container(color: Colors.grey[200]),
                        )
                      : Container(color: Colors.grey[200]),
                ),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBookInfo() {
    return Padding(
      padding: const EdgeInsets.all(24.0),
      child: Column(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          // Trạng thái mượn
          Container(
            padding: const EdgeInsets.symmetric(horizontal: 12, vertical: 6),
            decoration: BoxDecoration(
              color: _availableCopies > 0 ? Colors.green[50] : Colors.red[50],
              borderRadius: BorderRadius.circular(20),
              border: Border.all(
                color: _availableCopies > 0 ? Colors.green[200]! : Colors.red[200]!,
              ),
            ),
            child: Text(
              _availableCopies > 0 
                  ? 'Còn $_availableCopies quyển trên kệ' 
                  : 'Đã cho mượn hết',
              style: TextStyle(
                color: _availableCopies > 0 ? Colors.green[700] : Colors.red[700],
                fontWeight: FontWeight.bold,
                fontSize: 13,
              ),
            ),
          ),
          const SizedBox(height: 16),
          
          // Tiêu đề
          Text(
            widget.book.title,
            style: const TextStyle(fontSize: 26, fontWeight: FontWeight.bold, height: 1.3),
          ),
          const SizedBox(height: 8),
          
          // Tác giả
          Text(
            widget.book.author,
            style: TextStyle(fontSize: 18, color: Colors.grey[600], fontStyle: FontStyle.italic),
          ),
          
          const Padding(
            padding: EdgeInsets.symmetric(vertical: 24),
            child: Divider(),
          ),
          
          // Thông số
          const Text('Thông tin chi tiết', style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold)),
          const SizedBox(height: 16),
          _buildInfoRow('Nhà xuất bản', widget.book.publisher ?? 'Đang cập nhật'),
          _buildInfoRow('Năm xuất bản', widget.book.publishedYear?.toString() ?? 'Đang cập nhật'),
          _buildInfoRow('Mã ISBN', widget.book.isbn),
          _buildInfoRow('Số lượng tổng', '${widget.book.totalCopies} quyển'),
          _buildInfoRow('Đang cho mượn', '${widget.book.totalCopies - _availableCopies} quyển'),
          
          const SizedBox(height: 32),
          
          // Nút Pick-to-light
          Container(
            padding: const EdgeInsets.all(20),
            decoration: BoxDecoration(
              color: Colors.indigo[50],
              borderRadius: BorderRadius.circular(16),
              border: Border.all(color: Colors.indigo[100]!),
            ),
            child: Column(
              children: [
                Icon(Icons.place, size: 40, color: Colors.indigo[400]),
                const SizedBox(height: 12),
                const Text(
                  'Bạn không biết sách ở đâu?',
                  style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 8),
                Text(
                  'Nhấn nút bên dưới để bật đèn tín hiệu tại vị trí cuốn sách trên kệ.',
                  textAlign: TextAlign.center,
                  style: TextStyle(color: Colors.grey[700]),
                ),
                const SizedBox(height: 16),
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton.icon(
                    onPressed: _isLighting ? null : _triggerPickToLight,
                    icon: _isLighting 
                        ? const SizedBox(width: 20, height: 20, child: CircularProgressIndicator(strokeWidth: 2, color: Colors.white))
                        : const Icon(Icons.tips_and_updates),
                    label: Text(_isLighting ? 'Đang gửi tín hiệu...' : 'Nháy đèn tìm sách'),
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.amber[600],
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      elevation: 2,
                    ),
                  ),
                ),
              ],
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildInfoRow(String label, String value) {
    return Padding(
      padding: const EdgeInsets.only(bottom: 12),
      child: Row(
        crossAxisAlignment: CrossAxisAlignment.start,
        children: [
          SizedBox(
            width: 120,
            child: Text(label, style: TextStyle(color: Colors.grey[600], fontSize: 15)),
          ),
          Expanded(
            child: Text(value, style: const TextStyle(fontWeight: FontWeight.w500, fontSize: 15)),
          ),
        ],
      ),
    );
  }

  Widget _buildBottomBar() {
    return Container(
      padding: const EdgeInsets.all(20),
      decoration: BoxDecoration(
        color: Colors.white,
        boxShadow: [
          BoxShadow(
            color: Colors.black.withOpacity(0.05),
            offset: const Offset(0, -4),
            blurRadius: 16,
          ),
        ],
      ),
      child: SafeArea(
        child: SizedBox(
          height: 54,
          child: ElevatedButton(
            onPressed: (_availableCopies > 0 && !_isBorrowing) ? _borrowBook : null,
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.indigo[600],
              foregroundColor: Colors.white,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
              disabledBackgroundColor: Colors.grey[300],
            ),
            child: _isBorrowing
                ? const SizedBox(
                    width: 24,
                    height: 24,
                    child: CircularProgressIndicator(
                      strokeWidth: 2.5,
                      color: Colors.white,
                    ),
                  )
                : const Text(
                    'ĐĂNG KÝ MƯỢN SÁCH',
                    style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold, letterSpacing: 1),
                  ),
          ),
        ),
      ),
    );
  }
}
