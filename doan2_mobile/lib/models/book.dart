class Book {
  final int? id;
  final String title;
  final String author;
  final String isbn;
  final int availableCopies;
  final int totalCopies;
  final String? publisher;
  final int? publishedYear;
  final String? summary;
  final String? coverImageUrl;
  final EslTag? eslTag;

  Book({
    this.id,
    required this.title,
    required this.author,
    required this.isbn,
    required this.availableCopies,
    required this.totalCopies,
    this.publisher,
    this.publishedYear,
    this.summary,
    this.coverImageUrl,
    this.eslTag,
  });

  factory Book.fromJson(Map<String, dynamic> json) {
    return Book(
      id: json['id'],
      title: json['title'] ?? '',
      author: json['author'] ?? '',
      isbn: json['isbn'] ?? '',
      availableCopies: json['availableCopies'] ?? 0,
      totalCopies: json['totalCopies'] ?? json['availableCopies'] ?? 0,
      publisher: json['publisher'],
      publishedYear: json['publishedYear'],
      summary: json['summary'],
      coverImageUrl: json['coverImageUrl'],
      eslTag: json['eslTag'] != null ? EslTag.fromJson(json['eslTag']) : null,
    );
  }
}

class EslTag {
  final int? id;
  final String macAddress;
  final String? location;
  final int batteryLevel;
  final bool isOnline;
  final Book? book;

  EslTag({
    this.id,
    required this.macAddress,
    this.location,
    required this.batteryLevel,
    required this.isOnline,
    this.book,
  });

  factory EslTag.fromJson(Map<String, dynamic> json) {
    return EslTag(
      id: json['id'],
      macAddress: json['macAddress'] ?? '',
      location: json['location'],
      batteryLevel: json['batteryLevel'] ?? 100,
      isOnline: json['isOnline'] ?? false,
      book: json['book'] != null ? Book.fromJson(json['book']) : null,
    );
  }
}

class RfidTag {
  final int? id;
  final String epc;
  final String? currentLocation;
  final String? lastScannedAt;
  final Book? book;

  RfidTag({
    this.id,
    required this.epc,
    this.currentLocation,
    this.lastScannedAt,
    this.book,
  });

  factory RfidTag.fromJson(Map<String, dynamic> json) {
    return RfidTag(
      id: json['id'],
      epc: json['epc'] ?? '',
      currentLocation: json['currentLocation'],
      lastScannedAt: json['lastScannedAt'],
      book: json['book'] != null ? Book.fromJson(json['book']) : null,
    );
  }
}
