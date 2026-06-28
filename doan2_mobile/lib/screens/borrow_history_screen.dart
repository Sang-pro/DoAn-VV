import 'package:flutter/material.dart';
import 'package:provider/provider.dart';
import 'package:intl/intl.dart';
import '../models/borrow_record.dart';
import '../providers/auth_provider.dart';
import '../services/api_service.dart';

class BorrowHistoryScreen extends StatefulWidget {
  final int initialTabIndex; // 0 for "Đang mượn", 1 for "Lịch sử"

  const BorrowHistoryScreen({super.key, this.initialTabIndex = 0});

  @override
  _BorrowHistoryScreenState createState() => _BorrowHistoryScreenState();
}

class _BorrowHistoryScreenState extends State<BorrowHistoryScreen> {
  final ApiService _apiService = ApiService();
  List<BorrowRecord> _records = [];
  bool _isLoading = true;
  String _error = '';

  @override
  void initState() {
    super.initState();
    _fetchHistory();
  }

  Future<void> _fetchHistory() async {
    setState(() {
      _isLoading = true;
      _error = '';
    });

    final authProvider = Provider.of<AuthProvider>(context, listen: false);
    final user = authProvider.user;

    if (user == null || user.userCode == null) {
      setState(() {
        _error = 'Không tìm thấy thông tin tài khoản hoặc mã số người dùng.';
        _isLoading = false;
      });
      return;
    }

    try {
      final records = await _apiService.getBorrowHistory(user.userCode!);
      setState(() {
        _records = records;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  @override
  Widget build(BuildContext context) {
    return DefaultTabController(
      length: 2,
      initialIndex: widget.initialTabIndex,
      child: Scaffold(
        backgroundColor: Colors.grey[50],
        appBar: AppBar(
          title: const Text(
            'Quản lý mượn sách',
            style: TextStyle(fontWeight: FontWeight.bold, color: Colors.white),
          ),
          backgroundColor: Colors.indigo[600],
          foregroundColor: Colors.white,
          elevation: 0,
          bottom: const TabBar(
            labelColor: Colors.white,
            unselectedLabelColor: Colors.white70,
            indicatorColor: Colors.white,
            indicatorSize: TabBarIndicatorSize.tab,
            tabs: [
              Tab(
                icon: Icon(Icons.bookmark_outline),
                text: 'Đang mượn',
              ),
              Tab(
                icon: Icon(Icons.history),
                text: 'Lịch sử mượn',
              ),
            ],
          ),
        ),
        body: _isLoading
            ? const Center(child: CircularProgressIndicator())
            : _error.isNotEmpty
                ? _buildErrorWidget()
                : TabBarView(
                    children: [
                      _buildBorrowedTab(),
                      _buildHistoryTab(),
                    ],
                  ),
      ),
    );
  }

  Widget _buildErrorWidget() {
    return Center(
      child: Padding(
        padding: const EdgeInsets.all(24.0),
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            Icon(Icons.error_outline, size: 70, color: Colors.red[400]),
            const SizedBox(height: 16),
            Text(
              'Đã xảy ra lỗi',
              style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.grey[800]),
            ),
            const SizedBox(height: 8),
            Text(
              _error,
              textAlign: TextAlign.center,
              style: TextStyle(color: Colors.grey[600]),
            ),
            const SizedBox(height: 24),
            ElevatedButton.icon(
              onPressed: _fetchHistory,
              icon: const Icon(Icons.refresh),
              label: const Text('Thử lại'),
              style: ElevatedButton.styleFrom(
                backgroundColor: Colors.indigo[600],
                foregroundColor: Colors.white,
                shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
              ),
            ),
          ],
        ),
      ),
    );
  }

  Widget _buildBorrowedTab() {
    final activeRecords = _records.where((r) => r.status == 'BORROWED').toList();

    if (activeRecords.isEmpty) {
      return _buildEmptyState(
        icon: Icons.menu_book,
        title: 'Chưa mượn sách nào',
        message: 'Bạn hiện tại không có cuốn sách nào đang được mượn.',
      );
    }

    return RefreshIndicator(
      onRefresh: _fetchHistory,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: activeRecords.length,
        itemBuilder: (context, index) {
          final record = activeRecords[index];
          return _buildRecordCard(record, isActive: true);
        },
      ),
    );
  }

  Widget _buildHistoryTab() {
    if (_records.isEmpty) {
      return _buildEmptyState(
        icon: Icons.history,
        title: 'Chưa có hoạt động',
        message: 'Lịch sử mượn trả sách của bạn sẽ hiển thị ở đây.',
      );
    }

    return RefreshIndicator(
      onRefresh: _fetchHistory,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _records.length,
        itemBuilder: (context, index) {
          final record = _records[index];
          return _buildRecordCard(record, isActive: false);
        },
      ),
    );
  }

  Widget _buildEmptyState({
    required IconData icon,
    required String title,
    required String message,
  }) {
    return RefreshIndicator(
      onRefresh: _fetchHistory,
      child: SingleChildScrollView(
        physics: const AlwaysScrollableScrollPhysics(),
        child: Container(
          height: MediaQuery.of(context).size.height * 0.6,
          padding: const EdgeInsets.all(32),
          alignment: Alignment.center,
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Container(
                padding: const EdgeInsets.all(24),
                decoration: BoxDecoration(
                  color: Colors.indigo[50],
                  shape: BoxShape.circle,
                ),
                child: Icon(icon, size: 80, color: Colors.indigo[400]),
              ),
              const SizedBox(height: 24),
              Text(
                title,
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.grey[800]),
              ),
              const SizedBox(height: 8),
              Text(
                message,
                textAlign: TextAlign.center,
                style: TextStyle(fontSize: 14, color: Colors.grey[600], height: 1.4),
              ),
            ],
          ),
        ),
      ),
    );
  }

  Widget _buildRecordCard(BorrowRecord record, {required bool isActive}) {
    final dateFormat = DateFormat('dd/MM/yyyy');
    final now = DateTime.now();
    final difference = record.dueDate.difference(now).inDays;
    
    final bool isOverdue = record.status == 'OVERDUE' || 
        (record.status == 'BORROWED' && record.dueDate.isBefore(now));

    Color statusColor;
    String statusText;

    if (record.status == 'RETURNED') {
      statusColor = Colors.green;
      statusText = 'Đã trả';
    } else if (isOverdue) {
      statusColor = Colors.red;
      statusText = 'Quá hạn';
    } else {
      statusColor = Colors.orange;
      statusText = 'Đang mượn';
    }

    return Card(
      elevation: 2,
      margin: const EdgeInsets.only(bottom: 16),
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      child: ClipRRect(
        borderRadius: BorderRadius.circular(16),
        child: Container(
          decoration: BoxDecoration(
            border: Border(
              left: BorderSide(color: statusColor, width: 6),
            ),
          ),
          padding: const EdgeInsets.all(16),
          child: Row(
            crossAxisAlignment: CrossAxisAlignment.start,
            children: [
              // Book Cover Image or Placeholder
              ClipRRect(
                borderRadius: BorderRadius.circular(8),
                child: Container(
                  width: 70,
                  height: 100,
                  color: Colors.grey[100],
                  child: record.book.coverImageUrl != null && record.book.coverImageUrl!.isNotEmpty
                      ? Image.network(
                          record.book.coverImageUrl!,
                          fit: BoxFit.cover,
                          errorBuilder: (_, __, ___) => const Icon(Icons.book, size: 40, color: Colors.grey),
                        )
                      : const Icon(Icons.book, size: 40, color: Colors.grey),
                ),
              ),
              const SizedBox(width: 16),
              // Borrow Info
              Expanded(
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.start,
                  children: [
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Expanded(
                          child: Text(
                            record.book.title,
                            maxLines: 2,
                            overflow: TextOverflow.ellipsis,
                            style: const TextStyle(
                              fontSize: 16,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                        Container(
                          padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 4),
                          decoration: BoxDecoration(
                            color: statusColor.withOpacity(0.12),
                            borderRadius: BorderRadius.circular(12),
                          ),
                          child: Text(
                            statusText,
                            style: TextStyle(
                              color: statusColor,
                              fontSize: 12,
                              fontWeight: FontWeight.bold,
                            ),
                          ),
                        ),
                      ],
                    ),
                    const SizedBox(height: 4),
                    Text(
                      'Tác giả: ${record.book.author}',
                      style: TextStyle(color: Colors.grey[600], fontSize: 13),
                    ),
                    const SizedBox(height: 12),
                    Divider(height: 1, color: Colors.grey[200]),
                    const SizedBox(height: 12),
                    Row(
                      mainAxisAlignment: MainAxisAlignment.spaceBetween,
                      children: [
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              'Ngày mượn',
                              style: TextStyle(color: Colors.grey[500], fontSize: 11),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              dateFormat.format(record.borrowDate),
                              style: TextStyle(color: Colors.grey[800], fontSize: 13, fontWeight: FontWeight.w500),
                            ),
                          ],
                        ),
                        Column(
                          crossAxisAlignment: CrossAxisAlignment.start,
                          children: [
                            Text(
                              record.status == 'RETURNED' ? 'Ngày trả' : 'Hạn trả',
                              style: TextStyle(color: Colors.grey[500], fontSize: 11),
                            ),
                            const SizedBox(height: 2),
                            Text(
                              record.status == 'RETURNED'
                                  ? dateFormat.format(record.returnDate ?? now)
                                  : dateFormat.format(record.dueDate),
                              style: TextStyle(
                                color: record.status == 'RETURNED'
                                    ? Colors.grey[800]
                                    : isOverdue
                                        ? Colors.red
                                        : Colors.grey[800],
                                fontSize: 13,
                                fontWeight: FontWeight.bold,
                              ),
                            ),
                          ],
                        ),
                      ],
                    ),
                    if (record.status == 'BORROWED') ...[
                      const SizedBox(height: 10),
                      Text(
                        isOverdue
                            ? 'Đã quá hạn ${difference.abs()} ngày!'
                            : 'Còn $difference ngày để trả sách.',
                        style: TextStyle(
                          color: isOverdue ? Colors.red : Colors.grey[600],
                          fontSize: 12,
                          fontWeight: isOverdue ? FontWeight.bold : FontWeight.normal,
                          fontStyle: isOverdue ? FontStyle.normal : FontStyle.italic,
                        ),
                      ),
                    ]
                  ],
                ),
              ),
            ],
          ),
        ),
      ),
    );
  }
}
