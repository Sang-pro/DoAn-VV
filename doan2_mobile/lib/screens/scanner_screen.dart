import 'package:flutter/material.dart';
import 'package:mobile_scanner/mobile_scanner.dart';
import '../models/book.dart';
import '../services/api_service.dart';

class ScannerScreen extends StatefulWidget {
  const ScannerScreen({super.key});

  @override
  _ScannerScreenState createState() => _ScannerScreenState();
}

class _ScannerScreenState extends State<ScannerScreen> {
  final ApiService _apiService = ApiService();
  bool _isProcessing = false;
  
  final MobileScannerController _scannerController = MobileScannerController(
    detectionSpeed: DetectionSpeed.normal,
    facing: CameraFacing.back,
    torchEnabled: false,
  );

  @override
  void dispose() {
    _scannerController.dispose();
    super.dispose();
  }

  void _handleBarcode(BarcodeCapture capture) async {
    if (_isProcessing) return;

    final List<Barcode> barcodes = capture.barcodes;
    if (barcodes.isNotEmpty) {
      final String? isbn = barcodes.first.rawValue;
      if (isbn != null && isbn.isNotEmpty) {
        setState(() {
          _isProcessing = true;
        });

        // Pause scanner while processing
        _scannerController.stop();

        await _processScannedIsbn(isbn);
        
        setState(() {
          _isProcessing = false;
        });
        
        // Resume scanner if the dialog is closed and we are back
        _scannerController.start();
      }
    }
  }

  Future<void> _processScannedIsbn(String isbn) async {
    try {
      final books = await _apiService.getBooks();
      final book = books.firstWhere(
        (b) => b.isbn == isbn,
        orElse: () => throw Exception('Không tìm thấy sách với mã ISBN: $isbn'),
      );

      if (mounted) {
        await _showActionDialog(book);
      }
    } catch (e) {
      if (mounted) {
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.error_outline, color: Colors.white),
                const SizedBox(width: 8),
                Expanded(child: Text(e.toString().replaceAll('Exception: ', ''))),
              ],
            ),
            backgroundColor: Colors.red[700],
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  Future<void> _showActionDialog(Book book) async {
    return showModalBottomSheet(
      context: context,
      isScrollControlled: true,
      backgroundColor: Colors.transparent,
      builder: (context) {
        return _ActionBottomSheet(book: book, apiService: _apiService);
      },
    );
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.black,
      appBar: AppBar(
        title: const Text('Quét ISBN / Mã vạch'),
        backgroundColor: Colors.transparent,
        foregroundColor: Colors.white,
        elevation: 0,
        actions: [
          IconButton(
            icon: const Icon(Icons.flash_on, color: Colors.yellow),
            onPressed: () => _scannerController.toggleTorch(),
          ),
          IconButton(
            icon: const Icon(Icons.flip_camera_ios),
            onPressed: () => _scannerController.switchCamera(),
          ),
        ],
      ),
      extendBodyBehindAppBar: true,
      body: Stack(
        children: [
          MobileScanner(
            controller: _scannerController,
            onDetect: _handleBarcode,
          ),
          
          // Scanner Overlay Guide
          Center(
            child: Container(
              width: 300,
              height: 150,
              decoration: BoxDecoration(
                border: Border.all(color: Colors.greenAccent, width: 3),
                borderRadius: BorderRadius.circular(12),
              ),
            ),
          ),
          
          // Processing overlay
          if (_isProcessing)
            Container(
              color: Colors.black.withOpacity(0.6),
              child: Center(
                child: Column(
                  mainAxisSize: MainAxisSize.min,
                  children: [
                    const CircularProgressIndicator(color: Colors.greenAccent),
                    const SizedBox(height: 16),
                    Text(
                      'Đang xử lý mã vạch...',
                      style: const TextStyle(color: Colors.white, fontSize: 16, fontWeight: FontWeight.bold),
                    ),
                  ],
                ),
              ),
            ),
            
          // Helper Text
          Align(
            alignment: Alignment.bottomCenter,
            child: Padding(
              padding: const EdgeInsets.only(bottom: 60.0),
              child: Container(
                padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 12),
                decoration: BoxDecoration(
                  color: Colors.black.withOpacity(0.7),
                  borderRadius: BorderRadius.circular(20),
                ),
                child: const Text(
                  'Hướng camera vào mã vạch mặt sau sách',
                  style: TextStyle(color: Colors.white, fontSize: 14),
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }
}

class _ActionBottomSheet extends StatefulWidget {
  final Book book;
  final ApiService apiService;

  const _ActionBottomSheet({required this.book, required this.apiService});

  @override
  _ActionBottomSheetState createState() => _ActionBottomSheetState();
}

class _ActionBottomSheetState extends State<_ActionBottomSheet> {
  final _formKey = GlobalKey<FormState>();
  final TextEditingController _userCodeController = TextEditingController();
  bool _isLoading = false;
  String _operation = 'borrow'; // 'borrow' or 'return'

  @override
  void dispose() {
    _userCodeController.dispose();
    super.dispose();
  }

  Future<void> _executeTransaction() async {
    if (!_formKey.currentState!.validate()) return;
    
    final userCode = _userCodeController.text.trim().toUpperCase();
    setState(() => _isLoading = true);

    try {
      Map<String, dynamic> result;
      if (_operation == 'borrow') {
        result = await widget.apiService.borrowBook(widget.book.id!, userCode);
      } else {
        result = await widget.apiService.returnBook(widget.book.id!, userCode);
      }

      if (mounted) {
        Navigator.pop(context); // close sheet
        
        final String displayName = result['user']?['displayName'] ?? result['user']?['username'] ?? 'Bạn đọc';
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.check_circle, color: Colors.white),
                const SizedBox(width: 8),
                Expanded(
                  child: Text(
                    _operation == 'borrow' 
                      ? 'Đã cho mượn thành công cho $displayName!' 
                      : 'Đã nhận trả sách thành công từ $displayName!'
                  )
                ),
              ],
            ),
            backgroundColor: Colors.green[700],
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    } catch (e) {
      if (mounted) {
        setState(() => _isLoading = false);
        ScaffoldMessenger.of(context).showSnackBar(
          SnackBar(
            content: Row(
              children: [
                const Icon(Icons.error_outline, color: Colors.white),
                const SizedBox(width: 8),
                Expanded(child: Text(e.toString().replaceAll('Exception: ', ''))),
              ],
            ),
            backgroundColor: Colors.red[700],
            behavior: SnackBarBehavior.floating,
          ),
        );
      }
    }
  }

  @override
  Widget build(BuildContext context) {
    // padding for keyboard pop up
    final bottomInset = MediaQuery.of(context).viewInsets.bottom;
    
    return Container(
      decoration: const BoxDecoration(
        color: Colors.white,
        borderRadius: BorderRadius.vertical(top: Radius.circular(24)),
      ),
      padding: EdgeInsets.fromLTRB(24, 12, 24, 24 + bottomInset),
      child: SingleChildScrollView(
        child: Form(
          key: _formKey,
          child: Column(
            mainAxisSize: MainAxisSize.min,
            crossAxisAlignment: CrossAxisAlignment.stretch,
            children: [
              // Drag Handle
              Center(
                child: Container(
                  width: 40,
                  height: 5,
                  decoration: BoxDecoration(color: Colors.grey[300], borderRadius: BorderRadius.circular(10)),
                ),
              ),
              const SizedBox(height: 16),
              
              const Text(
                'Nghiệp vụ Mượn / Trả Sách',
                style: TextStyle(fontSize: 20, fontWeight: FontWeight.bold, color: Colors.indigo),
                textAlign: TextAlign.center,
              ),
              const SizedBox(height: 16),
              
              // Operation tab selector
              Container(
                decoration: BoxDecoration(
                  color: Colors.grey[100],
                  borderRadius: BorderRadius.circular(12),
                ),
                padding: const EdgeInsets.all(4),
                child: Row(
                  children: [
                    Expanded(
                      child: GestureDetector(
                        onTap: () => setState(() => _operation = 'borrow'),
                        child: Container(
                          decoration: BoxDecoration(
                            color: _operation == 'borrow' ? Colors.white : Colors.transparent,
                            borderRadius: BorderRadius.circular(8),
                            boxShadow: _operation == 'borrow' 
                              ? [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 4, offset: const Offset(0, 2))]
                              : null,
                          ),
                          padding: const EdgeInsets.symmetric(vertical: 10),
                          alignment: Alignment.center,
                          child: Text(
                            'CHO MƯỢN',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                              color: _operation == 'borrow' ? Colors.indigo : Colors.grey[600],
                            ),
                          ),
                        ),
                      ),
                    ),
                    Expanded(
                      child: GestureDetector(
                        onTap: () => setState(() => _operation = 'return'),
                        child: Container(
                          decoration: BoxDecoration(
                            color: _operation == 'return' ? Colors.white : Colors.transparent,
                            borderRadius: BorderRadius.circular(8),
                            boxShadow: _operation == 'return' 
                              ? [BoxShadow(color: Colors.black.withOpacity(0.05), blurRadius: 4, offset: const Offset(0, 2))]
                              : null,
                          ),
                          padding: const EdgeInsets.symmetric(vertical: 10),
                          alignment: Alignment.center,
                          child: Text(
                            'NHẬN TRẢ',
                            style: TextStyle(
                              fontWeight: FontWeight.bold,
                              fontSize: 13,
                              color: _operation == 'return' ? Colors.indigo : Colors.grey[600],
                            ),
                          ),
                        ),
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 16),
              
              // Book details card
              Container(
                padding: const EdgeInsets.all(12),
                decoration: BoxDecoration(
                  color: Colors.grey[50],
                  borderRadius: BorderRadius.circular(16),
                  border: Border.all(color: Colors.grey[200]!),
                ),
                child: Row(
                  children: [
                    Container(
                      width: 50,
                      height: 70,
                      decoration: BoxDecoration(
                        color: Colors.grey[200],
                        borderRadius: BorderRadius.circular(8),
                        image: widget.book.coverImageUrl != null
                            ? DecorationImage(image: NetworkImage(widget.book.coverImageUrl!), fit: BoxFit.cover)
                            : null,
                      ),
                      child: widget.book.coverImageUrl == null
                          ? const Icon(Icons.book, color: Colors.grey)
                          : null,
                    ),
                    const SizedBox(width: 12),
                    Expanded(
                      child: Column(
                        crossAxisAlignment: CrossAxisAlignment.start,
                        children: [
                          Text(
                            widget.book.title,
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 15),
                            maxLines: 1,
                            overflow: TextOverflow.ellipsis,
                          ),
                          const SizedBox(height: 2),
                          Text('Tác giả: ${widget.book.author}', style: TextStyle(color: Colors.grey[600], fontSize: 12)),
                          const SizedBox(height: 6),
                          Row(
                            children: [
                              Container(
                                padding: const EdgeInsets.symmetric(horizontal: 6, vertical: 2),
                                decoration: BoxDecoration(
                                  color: widget.book.availableCopies > 0 ? Colors.green[50] : Colors.red[50],
                                  borderRadius: BorderRadius.circular(6),
                                ),
                                child: Text(
                                  'Sẵn có: ${widget.book.availableCopies} cuốn',
                                  style: TextStyle(
                                    color: widget.book.availableCopies > 0 ? Colors.green[700] : Colors.red[700],
                                    fontWeight: FontWeight.bold,
                                    fontSize: 11,
                                  ),
                                ),
                              ),
                              const SizedBox(width: 8),
                              Text('ISBN: ${widget.book.isbn}', style: TextStyle(color: Colors.grey[500], fontSize: 11)),
                            ],
                          ),
                        ],
                      ),
                    ),
                  ],
                ),
              ),
              const SizedBox(height: 20),
              
              if (_operation == 'borrow' && widget.book.availableCopies <= 0)
                Container(
                  padding: const EdgeInsets.all(12),
                  decoration: BoxDecoration(
                    color: Colors.red[50],
                    borderRadius: BorderRadius.circular(12),
                    border: Border.all(color: Colors.red[100]!),
                  ),
                  child: Row(
                    children: [
                      Icon(Icons.warning_amber_rounded, color: Colors.red[700]),
                      const SizedBox(width: 8),
                      Expanded(
                        child: Text(
                          'Sách này đã được mượn hết. Không thể thực hiện cho mượn!',
                          style: TextStyle(color: Colors.red[800], fontWeight: FontWeight.w600, fontSize: 13),
                        ),
                      ),
                    ],
                  ),
                )
              else ...[
                // User Code Input Field
                TextFormField(
                  controller: _userCodeController,
                  autofocus: true,
                  textCapitalization: TextCapitalization.characters,
                  decoration: InputDecoration(
                    labelText: 'Mã số người dùng (User Code)',
                    hintText: 'Ví dụ: LIB12345',
                    prefixIcon: const Icon(Icons.person_outline_rounded, color: Colors.indigo),
                    border: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: const BorderSide(color: Colors.grey),
                    ),
                    focusedBorder: OutlineInputBorder(
                      borderRadius: BorderRadius.circular(16),
                      borderSide: const BorderSide(color: Colors.indigo, width: 2),
                    ),
                    filled: true,
                    fillColor: Colors.grey[50],
                  ),
                  validator: (value) {
                    if (value == null || value.trim().isEmpty) {
                      return 'Vui lòng nhập mã người dùng';
                    }
                    return null;
                  },
                ),
                const SizedBox(height: 24),
                
                // CTA Action Button
                SizedBox(
                  height: 56,
                  child: ElevatedButton(
                    onPressed: _isLoading ? null : _executeTransaction,
                    style: ElevatedButton.styleFrom(
                      backgroundColor: _operation == 'borrow' ? Colors.indigo[600] : Colors.green[600],
                      foregroundColor: Colors.white,
                      elevation: 4,
                      shape: RoundedRectangleBorder(borderRadius: BorderRadius.circular(16)),
                    ),
                    child: _isLoading
                        ? const CircularProgressIndicator(color: Colors.white)
                        : Text(
                            _operation == 'borrow' ? 'XÁC NHẬN CHO MƯỢN' : 'XÁC NHẬN NHẬN TRẢ',
                            style: const TextStyle(fontWeight: FontWeight.bold, fontSize: 16, letterSpacing: 1.1),
                          ),
                  ),
                ),
              ],
            ],
          ),
        ),
      ),
    );
  }
}
