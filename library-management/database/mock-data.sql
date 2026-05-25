USE library_db;

-- Thể loại
INSERT INTO
    categories (name, description)
VALUES (
        'Văn học',
        'Sách văn học trong và ngoài nước'
    ),
    (
        'Khoa học kỹ thuật',
        'Sách về khoa học và công nghệ'
    ),
    (
        'Kinh tế',
        'Sách kinh tế, kinh doanh, tài chính'
    ),
    (
        'Tâm lý - Kỹ năng sống',
        'Sách phát triển bản thân'
    ),
    (
        'Lịch sử',
        'Sách lịch sử Việt Nam và thế giới'
    );

-- Sách
INSERT INTO
    books (
        title,
        author,
        isbn,
        total_copies,
        available_copies,
        category_id
    )
VALUES (
        'Dế Mèn Phiêu Lưu Ký',
        'Tô Hoài',
        '9786041057081',
        5,
        5,
        1
    ),
    (
        'Số Đỏ',
        'Vũ Trọng Phụng',
        '9786041057082',
        4,
        4,
        1
    ),
    (
        'Cho Tôi Xin Một Vé Đi Tuổi Thơ',
        'Nguyễn Nhật Ánh',
        '9786041057083',
        6,
        6,
        1
    ),
    (
        'Tôi Thấy Hoa Vàng Trên Cỏ Xanh',
        'Nguyễn Nhật Ánh',
        '9786041057084',
        5,
        5,
        1
    ),
    (
        'Lập Trình Java Cơ Bản',
        'Nguyễn Văn A',
        '9786041057085',
        3,
        3,
        2
    ),
    (
        'Clean Code',
        'Robert C. Martin',
        '9786041057086',
        4,
        4,
        2
    ),
    (
        'Design Patterns',
        'Gang of Four',
        '9786041057087',
        3,
        3,
        2
    ),
    (
        'Đắc Nhân Tâm',
        'Dale Carnegie',
        '9786041057088',
        7,
        7,
        4
    ),
    (
        'Nhà Giả Kim',
        'Paulo Coelho',
        '9786041057089',
        6,
        6,
        4
    ),
    (
        'Nghĩ Giàu Làm Giàu',
        'Napoleon Hill',
        '9786041057090',
        5,
        5,
        3
    ),
    (
        'Khởi Nghiệp Tinh Gọn',
        'Eric Ries',
        '9786041057091',
        4,
        4,
        3
    ),
    (
        'Lịch Sử Việt Nam',
        'Nhiều tác giả',
        '9786041057092',
        3,
        3,
        5
    ),
    (
        'Sapiens',
        'Yuval Noah Harari',
        '9786041057093',
        5,
        5,
        5
    ),
    (
        'Atomic Habits',
        'James Clear',
        '9786041057094',
        6,
        6,
        4
    ),
    (
        'Spring Boot In Action',
        'Craig Walls',
        '9786041057095',
        3,
        3,
        2
    );

-- Users mẫu (mật khẩu đều là "123456" đã mã hóa BCrypt)
INSERT INTO
    users (
        username,
        password,
        email,
        full_name,
        role
    )
VALUES (
        'user1',
        '$2a$10$qUr4ddG3WqTYvZL95Zh69.sn.LCkQ4roHJnqVbpxcp/j4RLgFL34K',
        'user1@test.com',
        'Nguyen Van A',
        'READER'
    ),
    (
        'user2',
        '$2a$10$qUr4ddG3WqTYvZL95Zh69.sn.LCkQ4roHJnqVbpxcp/j4RLgFL34K',
        'user2@test.com',
        'Tran Thi B',
        'READER'
    ),
    (
        'user3',
        '$2a$10$qUr4ddG3WqTYvZL95Zh69.sn.LCkQ4roHJnqVbpxcp/j4RLgFL34K',
        'user3@test.com',
        'Le Van C',
        'READER'
    ),
    (
        'user4',
        '$2a$10$qUr4ddG3WqTYvZL95Zh69.sn.LCkQ4roHJnqVbpxcp/j4RLgFL34K',
        'user4@test.com',
        'Pham Thi D',
        'READER'
    ),
    (
        'user5',
        '$2a$10$qUr4ddG3WqTYvZL95Zh69.sn.LCkQ4roHJnqVbpxcp/j4RLgFL34K',
        'user5@test.com',
        'Hoang Van E',
        'READER'
    );

-- Lịch sử mượn sách (để AI có dữ liệu học)
-- user1: thích văn học + tâm lý
INSERT INTO
    borrow_records (
        user_id,
        book_id,
        borrow_date,
        due_date,
        return_date,
        status
    )
VALUES (
        2,
        1,
        '2026-01-01',
        '2026-01-15',
        '2026-01-14',
        'RETURNED'
    ),
    (
        2,
        2,
        '2026-01-10',
        '2026-01-24',
        '2026-01-22',
        'RETURNED'
    ),
    (
        2,
        3,
        '2026-01-20',
        '2026-02-03',
        '2026-02-01',
        'RETURNED'
    ),
    (
        2,
        8,
        '2026-02-01',
        '2026-02-15',
        '2026-02-14',
        'RETURNED'
    ),
    (
        2,
        9,
        '2026-02-10',
        '2026-02-24',
        '2026-02-20',
        'RETURNED'
    ),

-- user2: thích kỹ thuật + tâm lý
(
    3,
    5,
    '2026-01-05',
    '2026-01-19',
    '2026-01-18',
    'RETURNED'
),
(
    3,
    6,
    '2026-01-15',
    '2026-01-29',
    '2026-01-28',
    'RETURNED'
),
(
    3,
    7,
    '2026-01-25',
    '2026-02-08',
    '2026-02-07',
    'RETURNED'
),
(
    3,
    8,
    '2026-02-05',
    '2026-02-19',
    '2026-02-18',
    'RETURNED'
),
(
    3,
    14,
    '2026-02-15',
    '2026-03-01',
    '2026-02-28',
    'RETURNED'
),

-- user3: thích văn học + lịch sử
(
    4,
    1,
    '2026-01-03',
    '2026-01-17',
    '2026-01-16',
    'RETURNED'
),
(
    4,
    4,
    '2026-01-13',
    '2026-01-27',
    '2026-01-25',
    'RETURNED'
),
(
    4,
    12,
    '2026-01-23',
    '2026-02-06',
    '2026-02-05',
    'RETURNED'
),
(
    4,
    13,
    '2026-02-03',
    '2026-02-17',
    '2026-02-16',
    'RETURNED'
),

-- user4: thích kinh tế + tâm lý
(
    5,
    10,
    '2026-01-07',
    '2026-01-21',
    '2026-01-20',
    'RETURNED'
),
(
    5,
    11,
    '2026-01-17',
    '2026-01-31',
    '2026-01-30',
    'RETURNED'
),
(
    5,
    8,
    '2026-01-27',
    '2026-02-10',
    '2026-02-09',
    'RETURNED'
),
(
    5,
    14,
    '2026-02-07',
    '2026-02-21',
    '2026-02-20',
    'RETURNED'
),
(
    5,
    9,
    '2026-02-17',
    '2026-03-03',
    '2026-03-01',
    'RETURNED'
),

-- user5: thích kỹ thuật + kinh tế
(
    6,
    5,
    '2026-01-08',
    '2026-01-22',
    '2026-01-21',
    'RETURNED'
),
(
    6,
    6,
    '2026-01-18',
    '2026-02-01',
    '2026-01-31',
    'RETURNED'
),
(
    6,
    15,
    '2026-01-28',
    '2026-02-11',
    '2026-02-10',
    'RETURNED'
),
(
    6,
    10,
    '2026-02-08',
    '2026-02-22',
    '2026-02-21',
    'RETURNED'
),
(
    6,
    11,
    '2026-02-18',
    '2026-03-04',
    '2026-03-02',
    'RETURNED'
);

-- Dữ liệu mẫu cho reservations
-- user1 đặt giữ sách kỹ thuật (vì chưa mượn bao giờ)
INSERT INTO
    reservations (user_id, book_id, status)
VALUES (2, 5, 'PENDING'),
    (2, 6, 'PENDING'),

-- user2 đặt giữ sách văn học
(3, 3, 'PENDING'), (3, 4, 'FULFILLED'),

-- user3 đặt giữ sách kinh tế
(4, 10, 'PENDING'), (4, 11, 'CANCELLED'),

-- user4 đặt giữ sách kỹ thuật
(5, 7, 'PENDING'), (5, 15, 'FULFILLED'),

-- user5 đặt giữ sách văn học + lịch sử
(6, 2, 'PENDING'), (6, 12, 'NOTIFIED');

-- Dữ liệu mẫu cho feedback
-- Ghi nhận hành vi mượn/trả để AI học
-- user1
INSERT INTO
    feedback (
        user_id,
        book_id,
        event_type,
        weight
    )
VALUES (2, 1, 'BORROW', 1.0),
    (2, 1, 'RETURN', 1.0),
    (2, 2, 'BORROW', 1.0),
    (2, 2, 'RETURN', 1.0),
    (2, 3, 'BORROW', 1.0),
    (2, 3, 'RETURN', 1.0),
    (2, 8, 'BORROW', 1.0),
    (2, 8, 'RETURN', 1.5),
    (2, 9, 'BORROW', 1.0),
    (2, 9, 'RETURN', 1.5),

-- user2
(3, 5, 'BORROW', 1.0),
(3, 5, 'RETURN', 1.0),
(3, 6, 'BORROW', 1.0),
(3, 6, 'RETURN', 1.5),
(3, 7, 'BORROW', 1.0),
(3, 7, 'RETURN', 1.0),
(3, 8, 'BORROW', 1.0),
(3, 8, 'RETURN', 1.0),
(3, 14, 'BORROW', 1.0),
(3, 14, 'RETURN', 2.0),

-- user3
(4, 1, 'BORROW', 1.0),
(4, 1, 'RETURN', 1.0),
(4, 4, 'BORROW', 1.0),
(4, 4, 'RETURN', 1.5),
(4, 12, 'BORROW', 1.0),
(4, 12, 'RETURN', 1.0),
(4, 13, 'BORROW', 1.0),
(4, 13, 'RETURN', 2.0),

-- user4
(5, 10, 'BORROW', 1.0),
(5, 10, 'RETURN', 1.5),
(5, 11, 'BORROW', 1.0),
(5, 11, 'RETURN', 1.0),
(5, 8, 'BORROW', 1.0),
(5, 8, 'RETURN', 1.5),
(5, 14, 'BORROW', 1.0),
(5, 14, 'RETURN', 2.0),
(5, 9, 'BORROW', 1.0),
(5, 9, 'RETURN', 1.5),

-- user5
(6, 5, 'BORROW', 1.0),
(6, 5, 'RETURN', 1.5),
(6, 6, 'BORROW', 1.0),
(6, 6, 'RETURN', 2.0),
(6, 15, 'BORROW', 1.0),
(6, 15, 'RETURN', 1.5),
(6, 10, 'BORROW', 1.0),
(6, 10, 'RETURN', 1.0),
(6, 11, 'BORROW', 1.0),
(6, 11, 'RETURN', 1.5);