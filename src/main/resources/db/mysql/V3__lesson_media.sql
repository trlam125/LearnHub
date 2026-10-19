alter table learning_resource add column material_type varchar(12) default 'document' not null;
alter table learning_resource add column external_url varchar(2048) default '' not null;
alter table learning_resource add column embed_url varchar(2048) default '' not null;
alter table learning_resource add column content_type varchar(120) default 'application/pdf' not null;
alter table learning_resource add column size_bytes bigint default 0 not null;
alter table learning_resource add constraint ck_resource_material_type check (material_type in ('document', 'video'));
create index idx_resource_lesson_type on learning_resource(lesson_id, material_type, reviewed);

-- Update only exact default sample text, preserving edited learning content.
update course set summary='Bài học mẫu với từ vựng, ví dụ và bài tập luyện tập.' where summary='Nội dung mẫu để thử chức năng phần mềm.';
update course set summary='Bài học mẫu giúp bạn làm quen với từ vựng, thẻ ôn tập và bài luyện tiếng Anh.' where summary='Nội dung mẫu để thử bài học, từ vựng, flashcard và bài tập tiếng Anh.';
update course set description='Khóa học mẫu giúp bạn làm quen với cách học theo từng bài. Chương trình HSK đầy đủ được giáo viên công bố riêng.' where description='KHÓA MẪU: thay nội dung, học phí và lịch học trước khi tuyển sinh. Không phải giáo trình HSK hoàn chỉnh.';
update course set description='Khóa học mẫu gồm các bài luyện tiếng Anh cơ bản. Chương trình CEFR và IELTS đầy đủ được giáo viên công bố riêng.' where description='KHÓA MẪU: thay nội dung và học phí trước khi mở bán. Chưa phải giáo trình CEFR hay IELTS đầy đủ.';
update lesson set content='Bài học mẫu: đọc nội dung, luyện từ vựng và làm bài tập để củng cố kiến thức.' where content='Bài học minh họa chức năng. Hãy thay bằng nội dung do giáo viên biên soạn trước khi mở bán.';
update lesson set content='Bài học mẫu: đọc từ và ví dụ, nghe phát âm rồi luyện tập.' where content='Bài học mẫu: đọc từ và ví dụ, nghe phát âm rồi luyện tập. Giáo viên cần biên soạn giáo trình thực tế trước khi mở bán.';
update teacher set role_text='Hồ sơ giáo viên mẫu' where role_text='Hồ sơ mẫu — cập nhật trước khi mở bán';
update teacher set bio='Hồ sơ minh họa trong khu học tiếng Trung.' where bio='Đây là hồ sơ minh họa. Cập nhật tên, ảnh và các thông tin đã được xác minh của giáo viên thực tế.';
update teacher set bio='Hồ sơ minh họa trong khu học tiếng Anh.' where bio='Hồ sơ mẫu dành cho khu học tiếng Anh. Không phải thông tin hoặc chứng chỉ đã được xác minh của giáo viên thực tế.';
update study_class set features='Thông tin lớp học mẫu' where features='Thông tin mẫu, cần cập nhật trước khi tuyển sinh';
