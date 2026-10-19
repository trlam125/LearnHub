create table app_user (
 id bigint auto_increment primary key,
 name varchar(150) not null,
 email varchar(150) not null,
 password varchar(255) not null,
 role varchar(20) not null,
 verified bit not null,
 session_version bigint not null,
 active bit not null,
 ranking_visible bit not null,
 public_name varchar(40) not null,
 created_at datetime(6) not null,
 unique (email),
 check (email = lower(email))
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table teacher (
 id bigint auto_increment primary key,
 name varchar(120) not null,
 role_text varchar(160) not null,
 bio longtext not null,
 education longtext not null,
 certificates longtext not null,
 experience longtext not null,
 philosophy longtext not null,
 portrait_url varchar(500) not null,
 facebook varchar(500) not null,
 zalo varchar(500) not null,
 community varchar(500) not null,
 contact_email varchar(150) not null,
 published bit not null,
 position integer not null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table course (
 id bigint auto_increment primary key,
 title varchar(180) not null,
 slug varchar(100) not null,
 summary varchar(300) not null,
 description longtext not null,
 subject varchar(60) not null,
 teacher_name varchar(120) not null,
 kind varchar(12) not null,
 price bigint not null,
 access_days integer not null,
 published bit not null,
 created_at datetime(6) not null,
 unique (slug),
 check (price >= 1000),
 check (access_days > 0)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table chapter (
 id bigint auto_increment primary key,
 course_id bigint not null,
 title varchar(180) not null,
 position integer not null,
 foreign key (course_id) references course(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table lesson (
 id bigint auto_increment primary key,
 chapter_id bigint not null,
 title varchar(180) not null,
 content longtext not null,
 position integer not null,
 preview bit not null,
 published bit not null,
 foreign key (chapter_id) references chapter(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table learning_resource (
 id bigint auto_increment primary key,
 lesson_id bigint not null,
 title varchar(180) not null,
 stored_name varchar(100) not null,
 original_name varchar(200) not null,
 allow_download bit not null,
 reviewed bit not null,
 foreign key (lesson_id) references lesson(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table enrollment (
 id bigint auto_increment primary key,
 user_id bigint not null,
 course_id bigint not null,
 starts_at datetime(6) not null,
 expires_at datetime(6) not null,
 active bit not null,
 source_order binary(16),
 foreign key (user_id) references app_user(id),
 foreign key (course_id) references course(id),
 unique (user_id, course_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table lesson_progress (
 id bigint auto_increment primary key,
 user_id bigint not null,
 lesson_id bigint not null,
 completed_at datetime(6) not null,
 foreign key (user_id) references app_user(id),
 foreign key (lesson_id) references lesson(id),
 unique (user_id, lesson_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table quiz (
 id bigint auto_increment primary key,
 lesson_id bigint not null,
 title varchar(180) not null,
 minutes integer not null,
 max_attempts integer not null,
 pass_score numeric(4,2) not null,
 published bit not null,
 foreign key (lesson_id) references lesson(id),
 unique (lesson_id),
 check (minutes between 1 and 180),
 check (max_attempts > 0),
 check (pass_score between 0 and 10)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table question (
 id bigint auto_increment primary key,
 quiz_id bigint not null,
 text longtext not null,
 kind varchar(10) not null,
 points integer not null,
 explanation longtext not null,
 position integer not null,
 foreign key (quiz_id) references quiz(id),
 check (points > 0)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table choice_option (
 id bigint auto_increment primary key,
 question_id bigint not null,
 text varchar(500) not null,
 correct bit not null,
 foreign key (question_id) references question(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table quiz_attempt (
 id binary(16) primary key,
 user_id bigint not null,
 quiz_id bigint not null,
 started_at datetime(6) not null,
 deadline datetime(6) not null,
 submitted_at datetime(6),
 snapshot_json longtext not null,
 answers_json longtext not null,
 result_json longtext not null,
 score numeric(4,2),
 pass_score numeric(4,2) not null,
 open_key varchar(100),
 foreign key (user_id) references app_user(id),
 foreign key (quiz_id) references quiz(id),
 unique (open_key)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table purchase_order (
 id binary(16) primary key,
 user_id bigint not null,
 course_id bigint not null,
 amount bigint not null,
 access_days integer not null,
 code bigint not null,
 provider varchar(20) not null,
 status varchar(20) not null,
 link_id varchar(100) not null,
 checkout_url varchar(500) not null,
 created_at datetime(6) not null,
 paid_at datetime(6),
 pending_key varchar(100),
 foreign key (user_id) references app_user(id),
 foreign key (course_id) references course(id),
 unique (code),
 unique (pending_key)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table payment (
 id bigint auto_increment primary key,
 order_id binary(16) not null,
 reference varchar(150) not null,
 amount bigint not null,
 created_at datetime(6) not null,
 foreign key (order_id) references purchase_order(id),
 unique (order_id),
 unique (reference)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table audit_log (
 id bigint auto_increment primary key,
 actor_id bigint,
 action varchar(80) not null,
 target varchar(180) not null,
 detail longtext not null,
 created_at datetime(6) not null,
 foreign key (actor_id) references app_user(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table mail_job (
 id bigint auto_increment primary key,
 event_key varchar(180) not null,
 recipient varchar(150) not null,
 subject varchar(180) not null,
 body longtext not null,
 tries integer not null,
 available_at datetime(6) not null,
 sent_at datetime(6),
 created_at datetime(6) not null,
 unique (event_key)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table auth_token (
 id bigint auto_increment primary key,
 token_hash varchar(64) not null,
 user_id bigint not null,
 kind varchar(10) not null,
 expires_at datetime(6) not null,
 used_at datetime(6),
 foreign key (user_id) references app_user(id),
 unique (token_hash)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table learning_path (
 id bigint auto_increment primary key,
 slug varchar(100) not null,
 title varchar(160) not null,
 symbol varchar(4) not null,
 category varchar(20) not null,
 description longtext not null,
 published bit not null,
 position integer not null,
 unique (slug)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table learning_level (
 id bigint auto_increment primary key,
 path_id bigint not null,
 number integer not null,
 title varchar(120) not null,
 description varchar(300) not null,
 course_id bigint,
 published bit not null,
 foreign key (path_id) references learning_path(id),
 foreign key (course_id) references course(id),
 unique (path_id, number),
 unique (course_id),
 check (number between 1 and 9)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table word (
 id bigint auto_increment primary key,
 lesson_id bigint not null,
 hanzi varchar(80) not null,
 pinyin varchar(160) not null,
 meaning varchar(300) not null,
 sino_vietnamese varchar(120) not null,
 example_zh varchar(500) not null,
 example_vi varchar(500) not null,
 audio_url varchar(500) not null,
 position integer not null,
 foreign key (lesson_id) references lesson(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table word_progress (
 id bigint auto_increment primary key,
 user_id bigint not null,
 word_id bigint not null,
 saved bit not null,
 stage integer not null,
 next_review date not null,
 last_reviewed datetime(6),
 foreign key (user_id) references app_user(id),
 foreign key (word_id) references word(id),
 unique (user_id, word_id),
 check (stage between 0 and 6)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table study_activity (
 id bigint auto_increment primary key,
 user_id bigint not null,
 event_key varchar(150) not null,
 day date not null,
 xp integer not null,
 kind varchar(20) not null,
 created_at datetime(6) not null,
 foreign key (user_id) references app_user(id),
 unique (user_id, event_key),
 check (xp >= 0)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table study_class (
 id bigint auto_increment primary key,
 course_id bigint not null,
 teacher_id bigint not null,
 title varchar(160) not null,
 schedule varchar(250) not null,
 starts_on date,
 sessions integer not null,
 capacity integer not null,
 features longtext not null,
 accepting bit not null,
 published bit not null,
 foreign key (course_id) references course(id),
 foreign key (teacher_id) references teacher(id),
 check (capacity > 0),
 check (sessions > 0)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table consultation (
 id bigint auto_increment primary key,
 offering_id bigint not null,
 name varchar(120) not null,
 email varchar(150) not null,
 phone varchar(30) not null,
 message varchar(2000) not null,
 status varchar(20) not null,
 created_at datetime(6) not null,
 foreign key (offering_id) references study_class(id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table feedback (
 id bigint auto_increment primary key,
 user_id bigint not null,
 rating integer not null,
 message varchar(2000) not null,
 resolved bit not null,
 created_at datetime(6) not null,
 foreign key (user_id) references app_user(id),
 check (rating between 1 and 5)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table game_topic (
 id bigint auto_increment primary key,
 title varchar(120) not null,
 description varchar(300) not null,
 symbol varchar(4) not null,
 quiz_id bigint not null,
 published bit not null,
 position integer not null,
 foreign key (quiz_id) references quiz(id),
 unique (quiz_id)
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create table planned_product (
 id bigint auto_increment primary key,
 title varchar(120) not null,
 symbol varchar(4) not null,
 description longtext not null,
 published bit not null,
 position integer not null
) engine=InnoDB default charset=utf8mb4 collate=utf8mb4_bin;

create index activity_day_user_idx on study_activity(day,user_id);
create index mail_due_idx on mail_job(sent_at,available_at);
create index attempt_deadline_idx on quiz_attempt(submitted_at,deadline);
create index order_status_idx on purchase_order(status,created_at);
create index word_lesson_idx on word(lesson_id,position);
