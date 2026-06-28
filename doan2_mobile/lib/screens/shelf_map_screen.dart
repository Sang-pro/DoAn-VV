import 'dart:async';
import 'package:flutter/material.dart';
import '../models/book.dart';
import '../services/api_service.dart';

class ShelfMapScreen extends StatefulWidget {
  const ShelfMapScreen({super.key});

  @override
  _ShelfMapScreenState createState() => _ShelfMapScreenState();
}

class _ShelfMapScreenState extends State<ShelfMapScreen> {
  final ApiService _apiService = ApiService();
  List<EslTag> _shelves = [];
  bool _isLoading = true;
  String _error = '';
  Timer? _pollingTimer;

  @override
  void initState() {
    super.initState();
    _fetchShelves();
    // Automatic polling every 3 seconds for digital twin real-time sync
    _pollingTimer = Timer.periodic(const Duration(seconds: 3), (timer) {
      _fetchShelves(isSilent: true);
    });
  }

  @override
  void dispose() {
    _pollingTimer?.cancel();
    super.dispose();
  }

  Future<void> _fetchShelves({bool isSilent = false}) async {
    if (!isSilent) {
      setState(() {
        _isLoading = true;
        _error = '';
      });
    }

    try {
      final tags = await _apiService.getEslTags();
      
      // Filter tags that have a location assigned (representing physical shelves)
      final filtered = tags.where((t) => t.location != null && t.location!.isNotEmpty).toList();
      
      // Sort shelves by location name (e.g. A1, A2, B1...)
      filtered.sort((a, b) => a.location!.compareTo(b.location!));

      if (mounted) {
        setState(() {
          _shelves = filtered;
          _isLoading = false;
        });
      }
    } catch (e) {
      if (mounted && !isSilent) {
        setState(() {
          _error = e.toString().replaceAll('Exception: ', '');
          _isLoading = false;
        });
      }
    }
  }

  void _triggerPickToLight(int bookId, String location) async {
    try {
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Text('Đang gửi lệnh nháy đèn tìm sách tại Vị trí $location...'),
          duration: const Duration(seconds: 1),
        ),
      );
      
      await _apiService.pickToLight(bookId);
      
      ScaffoldMessenger.of(context).showSnackBar(
        SnackBar(
          content: Row(
            children: [
              const Icon(Icons.check_circle, color: Colors.white),
              const SizedBox(width: 8),
              Text('Đã bật đèn tại Vị trí $location thành công!'),
            ],
          ),
          backgroundColor: Colors.green,
          duration: const Duration(seconds: 3),
        ),
      );
    } catch (e) {
      ScaffoldMessenger.of(context).showSnackBar(
        const SnackBar(
          content: Text('Lỗi: Kệ sách không phản hồi. Vui lòng thử lại sau.'),
          backgroundColor: Colors.red,
        ),
      );
    }
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text(
          'Bản đồ Thư viện (Digital Twin)',
          style: TextStyle(fontWeight: FontWeight.bold, fontSize: 18),
        ),
        backgroundColor: Colors.indigo[600],
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: () => _fetchShelves(),
            tooltip: 'Làm mới bản đồ',
          ),
        ],
      ),
      body: _buildBody(),
    );
  }

  Widget _buildBody() {
    if (_isLoading && _shelves.isEmpty) {
      return Center(
        child: Column(
          mainAxisAlignment: MainAxisAlignment.center,
          children: [
            CircularProgressIndicator(color: Colors.indigo[600]),
            const SizedBox(height: 16),
            Text(
              'Đang tải sơ đồ kệ sách...',
              style: TextStyle(color: Colors.grey[600], fontWeight: FontWeight.w500),
            ),
          ],
        ),
      );
    }

    if (_error.isNotEmpty && _shelves.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.cloud_off, size: 64, color: Colors.grey),
              const SizedBox(height: 16),
              Text(
                'Lỗi kết nối máy chủ',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold, color: Colors.grey[800]),
              ),
              const SizedBox(height: 8),
              Text(
                _error,
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey[500]),
              ),
              const SizedBox(height: 24),
              ElevatedButton.icon(
                onPressed: () => _fetchShelves(),
                icon: const Icon(Icons.refresh),
                label: const Text('Thử lại'),
                style: ElevatedButton.styleFrom(
                  backgroundColor: Colors.indigo[600],
                  foregroundColor: Colors.white,
                  shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                ),
              ),
            ],
          ),
        ),
      );
    }

    if (_shelves.isEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(32.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              Icon(Icons.place_outlined, size: 72, color: Colors.indigo[200]),
              const SizedBox(height: 16),
              const Text(
                'Chưa có kệ sách nào',
                style: TextStyle(fontSize: 18, fontWeight: FontWeight.bold),
              ),
              const SizedBox(height: 8),
              Text(
                'Chưa có nhãn ESL nào được thiết lập vị trí hiển thị trong hệ thống.',
                textAlign: TextAlign.center,
                style: TextStyle(color: Colors.grey[500], height: 1.4),
              ),
            ],
          ),
        ),
      );
    }

    return Column(
      children: [
        // Information Banner
        Container(
          width: double.infinity,
          padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
          color: Colors.indigo[50],
          child: Row(
            children: [
              Icon(Icons.tips_and_updates_outlined, color: Colors.indigo[700], size: 20),
              const SizedBox(width: 8),
              Expanded(
                child: Text(
                  'Bản đồ đồng bộ thời gian thực. Bấm Nháy đèn để tìm sách trên kệ vật lý.',
                  style: TextStyle(color: Colors.indigo[900], fontSize: 12, fontWeight: FontWeight.w500),
                ),
              ),
            ],
          ),
        ),
        
        // Shelves Grid
        Expanded(
          child: GridView.builder(
            padding: const EdgeInsets.all(16),
            gridDelegate: const SliverGridDelegateWithFixedCrossAxisCount(
              crossAxisCount: 2,
              childAspectRatio: 0.76,
              crossAxisSpacing: 16,
              mainAxisSpacing: 16,
            ),
            itemCount: _shelves.length,
            itemBuilder: (context, index) {
              final shelf = _shelves[index];
              final hasBook = shelf.book != null;
              
              return Container(
                decoration: BoxDecoration(
                  color: Colors.white,
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(
                    color: hasBook ? Colors.indigo[200]! : Colors.grey[300]!,
                    width: hasBook ? 1.5 : 1.0,
                  ),
                  boxShadow: [
                    BoxShadow(
                      color: Colors.black.withOpacity(0.03),
                      blurRadius: 10,
                      offset: const Offset(0, 4),
                    ),
                  ],
                ),
                child: Column(
                  crossAxisAlignment: CrossAxisAlignment.stretch,
                  children: [
                    // Shelf Location Badge
                    Container(
                      padding: const EdgeInsets.symmetric(vertical: 8),
                      decoration: BoxDecoration(
                        color: hasBook ? Colors.indigo[600] : Colors.grey[700],
                        borderRadius: const BorderRadius.only(
                          topLeft: Radius.circular(14),
                          topRight: Radius.circular(14),
                        ),
                      ),
                      child: Text(
                        'KỆ: ${shelf.location}',
                        textAlign: TextAlign.center,
                        style: const TextStyle(
                          color: Colors.white,
                          fontWeight: FontWeight.bold,
                          fontFamily: 'monospace',
                          fontSize: 14,
                        ),
                      ),
                    ),
                    
                    Expanded(
                      child: Padding(
                        padding: const EdgeInsets.all(12.0),
                        child: hasBook 
                            ? _buildBookShelfContent(shelf)
                            : _buildEmptyShelfContent(),
                      ),
                    ),
                  ],
                ),
              );
            },
          ),
        ),
      ],
    );
  }

  Widget _buildBookShelfContent(EslTag shelf) {
    final book = shelf.book!;
    
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        // Book Cover or Icon
        Expanded(
          child: Container(
            margin: const EdgeInsets.only(bottom: 8),
            width: 70,
            decoration: BoxDecoration(
              borderRadius: BorderRadius.circular(8),
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.1),
                  blurRadius: 4,
                  offset: const Offset(0, 2),
                ),
              ],
            ),
            clipBehavior: Clip.antiAlias,
            child: book.coverImageUrl != null && book.coverImageUrl!.isNotEmpty
                ? Image.network(
                    book.coverImageUrl!,
                    fit: BoxFit.cover,
                    errorBuilder: (_, __, ___) => const Center(child: Icon(Icons.book, size: 36, color: Colors.indigo)),
                  )
                : const Center(child: Icon(Icons.book, size: 36, color: Colors.indigo)),
          ),
        ),
        
        // Book Title
        Text(
          book.title,
          maxLines: 1,
          overflow: TextOverflow.ellipsis,
          textAlign: TextAlign.center,
          style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 13),
        ),
        const SizedBox(height: 2),
        
        // Available Stock
        Text(
          'Còn lại: ${book.availableCopies} quyển',
          style: TextStyle(
            color: book.availableCopies > 0 ? Colors.indigo[700] : Colors.red,
            fontSize: 11,
            fontWeight: FontWeight.bold,
          ),
        ),
        const SizedBox(height: 8),
        
        // Pick-to-light button
        SizedBox(
          width: double.infinity,
          height: 34,
          child: ElevatedButton(
            onPressed: () => _triggerPickToLight(book.id!, shelf.location!),
            style: ElevatedButton.styleFrom(
              backgroundColor: Colors.amber[600],
              foregroundColor: Colors.white,
              padding: EdgeInsets.zero,
              elevation: 0,
              shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(8)),
            ),
            child: const Row(
              mainAxisAlignment: MainAxisAlignment.center,
              children: [
                Icon(Icons.tips_and_updates, size: 14),
                SizedBox(width: 4),
                Text('Nháy Đèn', style: TextStyle(fontSize: 11, fontWeight: FontWeight.bold)),
              ],
            ),
          ),
        ),
      ],
    );
  }

  Widget _buildEmptyShelfContent() {
    return Column(
      mainAxisAlignment: MainAxisAlignment.center,
      children: [
        Container(
          width: 48,
          height: 48,
          decoration: BoxDecoration(
            shape: BoxShape.circle,
            border: Border.all(color: Colors.grey[300]!, width: 2, style: BorderStyle.values[1]),
          ),
          child: Center(
            child: Text(
              '?',
              style: TextStyle(color: Colors.grey[350], fontSize: 20, fontWeight: FontWeight.bold),
            ),
          ),
        ),
        const SizedBox(height: 12),
        Text(
          'Kệ trống',
          style: TextStyle(color: Colors.grey[400], fontSize: 12, fontWeight: FontWeight.w500),
        ),
      ],
    );
  }
}
