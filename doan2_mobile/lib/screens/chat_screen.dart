import 'dart:convert';
import 'package:flutter/material.dart';
import 'package:shared_preferences/shared_preferences.dart';
import '../services/api_service.dart';

class ChatMessage {
  final String text;
  final bool isUser;
  
  ChatMessage({required this.text, required this.isUser});

  Map<String, dynamic> toJson() => {
    'text': text,
    'isUser': isUser,
  };

  factory ChatMessage.fromJson(Map<String, dynamic> json) => ChatMessage(
    text: json['text'] ?? '',
    isUser: json['isUser'] ?? false,
  );
}

class ChatScreen extends StatefulWidget {
  const ChatScreen({super.key});

  @override
  _ChatScreenState createState() => _ChatScreenState();
}

class _ChatScreenState extends State<ChatScreen> {
  final ApiService _apiService = ApiService();
  final TextEditingController _textController = TextEditingController();
  final ScrollController _scrollController = ScrollController();
  
  List<ChatMessage> _messages = [];
  bool _isLoading = false;

  @override
  void initState() {
    super.initState();
    _loadChatHistory();
  }

  Future<void> _loadChatHistory() async {
    final prefs = await SharedPreferences.getInstance();
    final String? historyJson = prefs.getString('ai_chat_history');
    if (historyJson != null) {
      try {
        final List<dynamic> parsed = jsonDecode(historyJson);
        setState(() {
          _messages = parsed.map((m) => ChatMessage.fromJson(m)).toList();
        });
        _scrollToBottom();
        return;
      } catch (e) {
        print('Error parsing chat history: $e');
      }
    }
    
    // Set default initial greeting if no history
    setState(() {
      _messages = [
        ChatMessage(text: 'Xin chào! Tôi là AI thủ thư của Hệ thống quản lý thư viện. Tôi có thể giúp gì cho bạn hôm nay?', isUser: false),
      ];
    });
  }

  Future<void> _saveChatHistory() async {
    final prefs = await SharedPreferences.getInstance();
    final String historyJson = jsonEncode(_messages.map((m) => m.toJson()).toList());
    await prefs.setString('ai_chat_history', historyJson);
  }

  Future<void> _clearChatHistory() async {
    final prefs = await SharedPreferences.getInstance();
    await prefs.remove('ai_chat_history');
    setState(() {
      _messages = [
        ChatMessage(text: 'Đã xóa hội thoại. Tôi có thể giúp gì thêm cho bạn?', isUser: false),
      ];
    });
    _scrollToBottom();
  }

  void _handleSubmitted(String text) async {
    if (text.trim().isEmpty) return;

    _textController.clear();
    
    setState(() {
      _messages.add(ChatMessage(text: text, isUser: true));
      _isLoading = true;
    });
    _saveChatHistory();
    
    _scrollToBottom();

    // Prepare message format for API
    List<Map<String, String>> apiMessages = _messages.map((m) {
      return {
        'role': m.isUser ? 'user' : 'assistant',
        'content': m.text,
      };
    }).toList();

    try {
      final responseText = await _apiService.chatWithAI(apiMessages);
      
      // Clean up thinking tags like <think>...</think> from deepseek output if any
      String cleanText = responseText.replaceAll(RegExp(r'<think>.*?</think>', dotAll: true), '').trim();
      if (cleanText.isEmpty) {
        cleanText = responseText; // fallback
      }

      setState(() {
        _messages.add(ChatMessage(text: cleanText, isUser: false));
        _isLoading = false;
      });
      _saveChatHistory();
    } catch (e) {
      setState(() {
        _messages.add(ChatMessage(text: 'Xin lỗi, tôi đang gặp sự cố kết nối. Vui lòng thử lại sau!', isUser: false));
        _isLoading = false;
      });
      _saveChatHistory();
    }
    
    _scrollToBottom();
  }

  void _scrollToBottom() {
    WidgetsBinding.instance.addPostFrameCallback((_) {
      if (_scrollController.hasClients) {
        _scrollController.animateTo(
          _scrollController.position.maxScrollExtent,
          duration: const Duration(milliseconds: 300),
          curve: Curves.easeOut,
        );
      }
    });
  }

  @override
  Widget build(BuildContext context) {
    return Scaffold(
      backgroundColor: Colors.grey[50],
      appBar: AppBar(
        title: const Row(
          children: [
            Icon(Icons.smart_toy, color: Colors.white),
            SizedBox(width: 8),
            Text('AI Thư viện', style: TextStyle(fontWeight: FontWeight.bold)),
          ],
        ),
        backgroundColor: Colors.indigo[600],
        foregroundColor: Colors.white,
        elevation: 1,
        actions: [
          IconButton(
            icon: const Icon(Icons.delete_sweep, color: Colors.white),
            onPressed: _clearChatHistory,
            tooltip: 'Xóa lịch sử chat',
          ),
        ],
      ),
      body: Column(
        children: [
          Expanded(
            child: ListView.builder(
              controller: _scrollController,
              padding: const EdgeInsets.all(16),
              itemCount: _messages.length,
              itemBuilder: (context, index) {
                final message = _messages[index];
                return _buildMessageBubble(message);
              },
            ),
          ),
          if (_isLoading)
            Padding(
              padding: const EdgeInsets.symmetric(horizontal: 24, vertical: 8),
              child: Align(
                alignment: Alignment.centerLeft,
                child: Row(
                  children: [
                    const SizedBox(
                      width: 16,
                      height: 16,
                      child: CircularProgressIndicator(strokeWidth: 2),
                    ),
                    const SizedBox(width: 8),
                    Text('AI đang suy nghĩ...', style: TextStyle(color: Colors.grey[600], fontSize: 13)),
                  ],
                ),
              ),
            ),
          Container(
            decoration: BoxDecoration(
              color: Colors.white,
              boxShadow: [
                BoxShadow(
                  color: Colors.black.withOpacity(0.05),
                  offset: const Offset(0, -4),
                  blurRadius: 10,
                )
              ],
            ),
            child: SafeArea(
              child: _buildTextComposer(),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildMessageBubble(ChatMessage message) {
    return Padding(
      padding: const EdgeInsets.symmetric(vertical: 8),
      child: Row(
        mainAxisAlignment: message.isUser ? MainAxisAlignment.end : MainAxisAlignment.start,
        crossAxisAlignment: CrossAxisAlignment.end,
        children: [
          if (!message.isUser)
            Container(
              margin: const EdgeInsets.only(right: 8),
              child: CircleAvatar(
                backgroundColor: Colors.indigo[100],
                radius: 16,
                child: Icon(Icons.smart_toy, size: 18, color: Colors.indigo[600]),
              ),
            ),
          Flexible(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16, vertical: 12),
              decoration: BoxDecoration(
                color: message.isUser ? Colors.indigo[600] : Colors.white,
                borderRadius: BorderRadius.only(
                  topLeft: const Radius.circular(16),
                  topRight: const Radius.circular(16),
                  bottomLeft: Radius.circular(message.isUser ? 16 : 4),
                  bottomRight: Radius.circular(message.isUser ? 4 : 16),
                ),
                border: message.isUser ? null : Border.all(color: Colors.grey[200]!),
              ),
              child: Text(
                message.text,
                style: TextStyle(
                  color: message.isUser ? Colors.white : Colors.black87,
                  fontSize: 15,
                  height: 1.4,
                ),
              ),
            ),
          ),
        ],
      ),
    );
  }

  Widget _buildTextComposer() {
    return Container(
      padding: const EdgeInsets.symmetric(horizontal: 8, vertical: 8),
      child: Row(
        children: [
          Expanded(
            child: Container(
              padding: const EdgeInsets.symmetric(horizontal: 16),
              decoration: BoxDecoration(
                color: Colors.grey[100],
                borderRadius: BorderRadius.circular(24),
              ),
              child: TextField(
                controller: _textController,
                onSubmitted: _handleSubmitted,
                decoration: const InputDecoration(
                  hintText: 'Nhập tin nhắn cho AI...',
                  border: InputBorder.none,
                ),
                maxLines: null,
                keyboardType: TextInputType.multiline,
                textInputAction: TextInputAction.send,
              ),
            ),
          ),
          const SizedBox(width: 8),
          Container(
            decoration: BoxDecoration(
              color: Colors.indigo[600],
              shape: BoxShape.circle,
            ),
            child: IconButton(
              icon: const Icon(Icons.send),
              color: Colors.white,
              onPressed: () => _handleSubmitted(_textController.text),
            ),
          ),
        ],
      ),
    );
  }
}
