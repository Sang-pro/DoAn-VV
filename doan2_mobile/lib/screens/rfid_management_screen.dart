import 'package:flutter/material.dart';
import '../models/book.dart';
import '../services/api_service.dart';

class RfidManagementScreen extends StatefulWidget {
  const RfidManagementScreen({super.key});

  @override
  _RfidManagementScreenState createState() => _RfidManagementScreenState();
}

class _RfidManagementScreenState extends State<RfidManagementScreen> {
  final ApiService _apiService = ApiService();
  List<RfidTag> _tags = [];
  List<Book> _books = [];
  bool _isLoading = true;
  String _error = '';
  String _searchQuery = '';

  @override
  void initState() {
    super.initState();
    _fetchData();
  }

  Future<void> _fetchData() async {
    setState(() {
      _isLoading = true;
      _error = '';
    });
    try {
      final results = await Future.wait([
        _apiService.getRfidTags(),
        _apiService.getBooks(),
      ]);
      setState(() {
        _tags = results[0] as List<RfidTag>;
        _books = results[1] as List<Book>;
        _isLoading = false;
      });
    } catch (e) {
      setState(() {
        _error = e.toString().replaceAll('Exception: ', '');
        _isLoading = false;
      });
    }
  }

  Future<void> _deleteTag(int id) async {
    final confirm = await showDialog<bool>(
      context: context,
      builder: (context) => AlertDialog(
        title: const Text('Xác nhận xóa'),
        content: const Text('Bạn có chắc chắn muốn xóa liên kết mã dán RFID này?'),
        shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
        actions: [
          TextButton(
            onPressed: () => Navigator.pop(context, false),
            child: const Text('Hủy', style: TextStyle(color: Colors.grey)),
          ),
          ElevatedButton(
            onPressed: () => Navigator.pop(context, true),
            style: ElevatedButton.styleFrom(backgroundColor: Colors.red[600], foregroundColor: Colors.white),
            child: const Text('Xóa'),
          ),
        ],
      ),
    );

    if (confirm == true) {
      try {
        await _apiService.deleteRfidTag(id);
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          const SnackBar(content: Text('Đã gỡ mã RFID thành công!'), backgroundColor: Colors.green),
        );
        _fetchData();
      } catch (e) {
        if (!mounted) return;
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(content: Text('Lỗi: ${e.toString()}'), backgroundColor: Colors.red),
        );
      }
    }
  }

  void _showRegisterDialog() {
    final epcController = TextEditingController();
    Book? selectedBook;
    String? dialogError;

    showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      shape: const RoundedRectangleBorder(
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      builder: (context) => StatefulBuilder(
        builder: (context, setModalState) => Padding(
          padding: EdgeInsets.only(
            bottom: MediaQuery.of(context).viewInsets.bottom,
            left: 20,
            right: 20,
            top: 20,
          ),
          child: SingleChildScrollView(
            child: Column(
              mainAxisSize: MainAxisSize.min,
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                Row(
                  mainAxisAlignment: MainAxisAlignment.spaceBetween,
                  children: [
                    const Text(
                      'Đăng ký mã dán RFID',
                      style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold),
                    ),
                    IconButton(
                      icon: const Icon(Icons.close),
                      onPressed: () => Navigator.pop(context),
                    ),
                  ],
                ),
                const SizedBox(height: 16),
                if (dialogError != null) ...[
                  Container(
                    width: double.infinity,
                    padding: const EdgeInsets.all(12),
                    decoration: BoxDecoration(
                      color: Colors.red[50],
                      borderRadius: BorderRadius.circular(12),
                      border: Border.all(color: Colors.red[100]!),
                    ),
                    child: Text(
                      dialogError!,
                      style: TextStyle(color: Colors.red[700], fontSize: 13, fontWeight: FontWeight.w500),
                    ),
                  ),
                  const SizedBox(height: 12),
                ],
                TextField(
                  controller: epcController,
                  decoration: InputDecoration(
                    labelText: 'Mã EPC (RFID Code)',
                    hintText: 'Nhập mã EPC của thẻ',
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                    prefixIcon: const Icon(Icons.tag),
                  ),
                  style: const TextStyle(fontFamily: 'monospace', fontWeight: FontWeight.bold),
                ),
                const SizedBox(height: 16),
                DropdownButtonFormField<Book>(
                  initialValue: selectedBook,
                  decoration: InputDecoration(
                    labelText: 'Chọn sách liên kết',
                    border: OutlineInputBorder(borderRadius: BorderRadius.circular(12)),
                    prefixIcon: const Icon(Icons.book),
                  ),
                  items: _books.map((book) {
                    return DropdownMenuItem<Book>(
                      value: book,
                      child: Container(
                        constraints: const BoxConstraints(maxWidth: 220),
                        child: Text(
                          book.title,
                          overflow: TextOverflow.ellipsis,
                          style: const TextStyle(fontSize: 14),
                        ),
                      ),
                    );
                  }).toList(),
                  onChanged: (value) {
                    setModalState(() {
                      selectedBook = value;
                    });
                  },
                ),
                const SizedBox(height: 24),
                SizedBox(
                  width: double.infinity,
                  height: 50,
                  child: ElevatedButton(
                    onPressed: () async {
                      if (epcController.text.trim().isEmpty) {
                        setModalState(() {
                          dialogError = 'Vui lòng nhập mã EPC';
                        });
                        return;
                      }
                      if (selectedBook == null) {
                        setModalState(() {
                          dialogError = 'Vui lòng chọn sách liên kết';
                        });
                        return;
                      }

                      try {
                        await _apiService.registerRfidTag(
                          epcController.text.trim(),
                          selectedBook!.id!,
                        );
                        if (!mounted) return;
                        Navigator.pop(context);
                        ScaffoldMessenger.of(context).showSnackBar(
                          const SnackBar(content: Text('Đăng ký mã RFID thành công!'), backgroundColor: Colors.green),
                        );
                        _fetchData();
                      } catch (e) {
                        setModalState(() {
                          dialogError = e.toString().replaceAll('Exception: ', '');
                        });
                      }
                    },
                    style: ElevatedButton.styleFrom(
                      backgroundColor: Colors.indigo[600],
                      foregroundColor: Colors.white,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(12)),
                      elevation: 2,
                    ),
                    child: const Text('Lưu thông tin', style: TextStyle(fontSize: 16, fontWeight: FontWeight.bold)),
                  ),
                ),
                const SizedBox(height: 20),
              ],
            ),
          ),
        ),
      ),
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Text('Quản lý mã dán RFID', style: TextStyle(fontWeight: FontWeight.bold)),
        backgroundColor: Colors.indigo[600],
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.refresh),
            onPressed: _fetchData,
          ),
        ],
      ),
      body: _buildBody(),
      floatingActionButton: FloatingActionButton(
        onPressed: _showRegisterDialog,
        backgroundColor: Colors.indigo[600],
        foregroundColor: Colors.white,
        child: const Icon(Icons.add),
      ),
    );
  }

  Widget _buildBody() {
    if (_isLoading) {
      return const Center(child: CircularProgressIndicator());
    }

    if (_error.isNotEmpty) {
      return Center(
        child: Padding(
          padding: const EdgeInsets.all(24.0),
          child: Column(
            mainAxisAlignment: MainAxisAlignment.center,
            children: [
              const Icon(Icons.error_outline, size: 60, color: Colors.red),
              const SizedBox(height: 16),
              Text('Lỗi: $_error', textAlign: TextAlign.center, style: const TextStyle(color: Colors.red)),
              const SizedBox(height: 16),
              ElevatedButton(onPressed: _fetchData, child: const Text('Thử lại')),
            ],
          ),
        ),
      );
    }

    final filteredTags = _tags.filter((tag) {
      final query = _searchQuery.toLowerCase();
      final title = tag.book?.title.toLowerCase() ?? '';
      final author = tag.book?.author.toLowerCase() ?? '';
      final epc = tag.epc.toLowerCase();
      final loc = tag.currentLocation?.toLowerCase() ?? '';
      return title.contains(query) || author.contains(query) || epc.contains(query) || loc.contains(query);
    }).toList();

    return Column(
      children: [
        // Search bar
        Container(
          padding: const EdgeInsets.all(16),
          color: Colors.white,
          child: TextField(
            onChanged: (value) {
              setState(() {
                _searchQuery = value;
              });
            },
            decoration: InputDecoration(
              hintText: 'Tìm kiếm theo mã EPC, tên sách, vị trí...',
              prefixIcon: const Icon(Icons.search),
              contentPadding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              border: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey[300]!),
              ),
              enabledBorder: OutlineInputBorder(
                borderRadius: BorderRadius.circular(12),
                borderSide: BorderSide(color: Colors.grey[200]!),
              ),
            ),
          ),
        ),
        
        // List of RFID Tags
        Expanded(
          child: filteredTags.isEmpty
              ? const Center(
                  child: Text('Không tìm thấy mã dán RFID nào', style: TextStyle(fontSize: 16, color: Colors.grey)),
                )
              : RefreshIndicator(
                  onRefresh: _fetchData,
                  child: ListView.builder(
                    padding: const EdgeInsets.all(16),
                    itemCount: filteredTags.length,
                    itemBuilder: (context, index) {
                      final tag = filteredTags[index];
                      return _buildTagCard(tag);
                    },
                  ),
                ),
        ),
      ],
    );
  }

  Widget _buildTagCard(RfidTag tag) {
    return Card(
      elevation: 1,
      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
      margin: const EdgeInsets.only(bottom: 12),
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
                    Icon(Icons.label_outline, color: Colors.indigo[600], size: 24),
                    const SizedBox(width: 10),
                    Column(
                      crossAxisAlignment: CrossAxisAlignment.start,
                      children: [
                        Text(
                          tag.epc,
                          style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold, fontFamily: 'monospace'),
                        ),
                        const SizedBox(height: 2),
                        const Text('Thẻ RFID sách', style: TextStyle(color: Colors.grey, fontSize: 11)),
                      ],
                    ),
                  ],
                ),
                IconButton(
                  icon: const Icon(Icons.delete_outline, color: Colors.red),
                  onPressed: () => tag.id != null ? _deleteTag(tag.id!) : null,
                ),
              ],
            ),
            const Divider(height: 16),
            Row(
              crossAxisAlignment: CrossAxisAlignment.start,
              children: [
                const Icon(Icons.book, size: 16, color: Colors.grey),
                const SizedBox(width: 8),
                Expanded(
                  child: Column(
                    crossAxisAlignment: CrossAxisAlignment.start,
                    children: [
                      Text(
                        tag.book?.title ?? 'Sách không xác định',
                        style: const TextStyle(fontSize: 14, fontWeight: FontWeight.bold),
                      ),
                      const SizedBox(height: 4),
                      Text(
                        'Tác giả: ${tag.book?.author ?? 'N/A'} | ISBN: ${tag.book?.isbn ?? 'N/A'}',
                        style: TextStyle(color: Colors.grey[600], fontSize: 12),
                      ),
                    ],
                  ),
                ),
              ],
            ),
            const SizedBox(height: 10),
            Row(
              children: [
                const Icon(Icons.location_on_outlined, size: 16, color: Colors.grey),
                const SizedBox(width: 8),
                Text('Vị trí: ${tag.currentLocation ?? "Chưa quét"}', style: TextStyle(color: Colors.grey[700], fontSize: 12)),
              ],
            ),
            if (tag.lastScannedAt != null) ...[
              const SizedBox(height: 6),
              Row(
                children: [
                  const Icon(Icons.access_time_outlined, size: 16, color: Colors.grey),
                  const SizedBox(width: 8),
                  Text(
                    'Lần cuối quét: ${tag.lastScannedAt != null ? DateTime.parse(tag.lastScannedAt!).toLocal().toString().substring(0, 16) : 'N/A'}',
                    style: TextStyle(color: Colors.grey[700], fontSize: 12),
                  ),
                ],
              ),
            ],
          ],
        ),
      ),
    );
  }
}

extension IterableExtension<T> on Iterable<T> {
  Iterable<T> filter(bool Function(T) test) {
    return where(test);
  }
}
