import 'package:flutter/material.dart';
import '../models/book.dart';
import '../services/api_service.dart';

class EslManagementScreen extends StatefulWidget {
  const EslManagementScreen({super.key});

  @override
  _EslManagementScreenState createState() => _EslManagementScreenState();
}

class _EslManagementScreenState extends State<EslManagementScreen> {
  final ApiService _apiService = ApiService();
  List<EslTag> _tags = [];
  bool _isLoading = true;
  String _error = '';

  @override
  void initState() {
    super.initState();
    _fetchTags();
  }

  Future<void> _fetchTags() async {
    setState(() {
      _isLoading = true;
      _error = '';
    });
    try {
      final tags = await _apiService.getEslTags();
      setState(() {
        _tags = tags;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  void _triggerPickToLight(int? bookId) async {
    if (bookId == null) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Thẻ này chưa được gán cho sách nào'), backgroundColor: Colors.orange),
      );
      return;
    }
    
    try {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đang gửi tín hiệu...'), duration: Duration(seconds: 1)),
      );
      await _apiService.pickToLight(bookId);
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(content: Text('Đã bật đèn ESL thành công!'), backgroundColor: Colors.green),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(content: Text('Lỗi: Thẻ ESL không phản hồi'), backgroundColor: Colors.red[700]),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text('Quản lý Thiết bị ESL', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.indigo[700],
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _fetchTags,
          ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error.isNotEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            const Icon(Icons.error_outline, size: 60, color: Colors.red),
            const SizedBox(height: 16),
            Text('Lỗi: $_error', style: const TextStyle(color: Colors.red)),
            const SizedBox(height: 16),
            ElevatedButton(onPressed: _fetchTags, child: const Text('Thử lại')),
          ],
        ),
      );
    }

    if (_tags.isEmpty) {
      return const Center(
        child: Text('Chưa có thẻ ESL nào trong hệ thống', style: TextStyle(fontSize: 16, color: Colors.grey)),
      );
    }

    return RefreshIndicator(
      onRefresh: _fetchTags,
      child: ListView.builder(
        padding: const EdgeInsets.all(16),
        itemCount: _tags.length,
        itemBuilder: (context, index) {
          final tag = _tags[index];
          return _buildTagCard(tag);
        },
      ),
    );
  }

  Widget _buildTagCard(EslTag tag) {
    final bool isOnline = tag.isOnline;
    final Color statusColor = isOnline ? Colors.green : Colors.grey;

    return Card(
      elevation: 2,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      margin: const EdgeInsets.only(bottom: 16),
      child: Padding(
        padding: const EdgeInsets.all(16.0),
        child: Column(
          crossAxisAlignment: CrossAxisAlignment.start,
          children: [
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Row(
                  children: [
                    Icon(Icons.important_devices, color: Colors.indigo[600], size: 28),
                    const SizedBox(width: 12),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          tag.macAddress,
                          style: const TextStyle(fontSize: 16, fontWeight: FontWeight.bold),
                        ),
                        const SizedBox(height: 4),
                        Row(
                          children: [
                            Container(
                              width: 8,
                              height: 8,
                              decoration: BoxDecoration(shape: BoxShape.circle, color: statusColor),
                            ),
                            const SizedBox(width: 4),
                            Text(
                              isOnline ? 'Online' : 'Offline',
                              style: TextStyle(color: statusColor, fontSize: 12, fontWeight: FontWeight.bold),
                            ),
                          ],
                        ),
                      ],
                    ),
                  ],
                ),
                Container(
                  padding: const EdgeInsets.symmetric(horizontal: 10, vertical: 6),
                  decoration: BoxDecoration(
                    color: tag.batteryLevel > 20 ? Colors.green[50] : Colors.red[50],
                    borderRadius: BorderRadius.circular(20),
                  ),
                  child: Row(
                    children: [
                      Icon(
                        tag.batteryLevel > 80 ? Icons.battery_full :
                        tag.batteryLevel > 20 ? Icons.battery_5_bar : Icons.battery_alert,
                        size: 16,
                        color: tag.batteryLevel > 20 ? Colors.green[700] : Colors.red[700],
                      ),
                      const SizedBox(width: 4),
                      Text(
                        '${tag.batteryLevel}%',
                        style: TextStyle(
                          color: tag.batteryLevel > 20 ? Colors.green[700] : Colors.red[700],
                          fontWeight: FontWeight.bold,
                          fontSize: 12,
                        ),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const Divider(height: 24),
            Row(
              children: [
                const Icon(Icons.location_on_outlined, size: 16, color: Colors.grey),
                const SizedBox(width: 8),
                Text('Vị trí: ${tag.location ?? "Chưa cấu hình"}', style: TextStyle(color: Colors.grey[700])),
              ],
            ),
            const SizedBox(height: 8),
            Row(
              mainAxisAlignment: MainAxisAlignment.spaceBetween,
              children: [
                Expanded(
                  child: Row(
                    children: [
                      const Icon(Icons.book_outlined, size: 16, color: Colors.grey),
                      const SizedBox(width: 8),
                      // We don't have book.title directly in EslTag from JSON usually unless backend joins it.
                      // Here we just display a generic "Đã gán sách" if it has bookId (not directly available in current EslTag model, but we will test it).
                      Text(
                        'Trạng thái: Đang hoạt động',
                        style: TextStyle(color: Colors.grey[700]),
                      ),
                    ],
                  ),
                ),
                ElevatedButton.icon(
                  onPressed: isOnline ? () {
                    // This assumes backend allows sending pick to light via bookId, but we might not have bookId in tag natively without a field.
                    // If tag has id, we might need a direct tag pick-to-light API, but for now we use a generic prompt.
                    ScaffoldMessenger.of(context).showSnackBar(
                      const SnackBar(content: Text('Tính năng nháy đèn từ Admin đang hoàn thiện.')),
                    );
                  } : null,
                  icon: const Icon(Icons.lightbulb_outline, size: 18),
                  label: const Text('Test Đèn'),
                  style: ElevatedButton.styleFrom(
                    backgroundColor: Colors.orange[50],
                    foregroundColor: Colors.orange[800],
                    elevation: 0,
                  ),
                ),
              ],
            ),
          ],
        ),
      ),
    );
  }
}
