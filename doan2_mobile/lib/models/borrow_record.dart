import 'book.dart';

class BorrowRecord {
  final int? id;
  final Book book;
  final DateTime borrowDate;
  final DateTime dueDate;
  final DateTime? returnDate;
  final String status; // BORROWED, RETURNED, OVERDUE

  BorrowRecord({
    this.id,
    required this.book,
    required this.borrowDate,
    required this.dueDate,
    this.returnDate,
    required this.status,
  });

  factory BorrowRecord.fromJson(Map<String, dynamic> json) {
    return BorrowRecord(
      id: json['id'],
      book: Book.fromJson(json['book']),
      borrowDate: DateTime.parse(json['borrowDate']),
      dueDate: DateTime.parse(json['dueDate']),
      returnDate: json['returnDate'] != null ? DateTime.parse(json['returnDate']) : null,
      status: json['status'] ?? 'BORROWED',
    );
  }
}
