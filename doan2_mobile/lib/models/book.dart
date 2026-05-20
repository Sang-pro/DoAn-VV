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

  EslTag({
    this.id,
    required this.macAddress,
    this.location,
    required this.batteryLevel,
    required this.isOnline,
  });

  factory EslTag.fromJson(Map<String, dynamic> json) {
    return EslTag(
      id: json['id'],
      macAddress: json['macAddress'] ?? '',
      location: json['location'],
      batteryLevel: json['batteryLevel'] ?? 100,
      isOnline: json['isOnline'] ?? false,
    );
  }
}
