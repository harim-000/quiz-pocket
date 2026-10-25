CREATE TABLE app_users (
 id UUID PRIMARY KEY, email VARCHAR(254) NOT NULL UNIQUE,
 display_name VARCHAR(60) NOT NULL, password_hash VARCHAR(100) NOT NULL
);
CREATE TABLE subjects (
 id UUID PRIMARY KEY, owner_id UUID NOT NULL REFERENCES app_users(id),
 name VARCHAR(60) NOT NULL, UNIQUE(owner_id,name)
);
CREATE INDEX subjects_owner_idx ON subjects(owner_id);
CREATE TABLE questions (
 id UUID PRIMARY KEY, subject_id UUID NOT NULL REFERENCES subjects(id) ON DELETE CASCADE,
 type VARCHAR(10) NOT NULL CHECK(type IN ('MC','SHORT','ESSAY','OX')),
 title TEXT NOT NULL, answer TEXT NOT NULL, explanation TEXT NOT NULL
);
CREATE INDEX questions_subject_idx ON questions(subject_id);
CREATE TABLE question_choices (
 question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
 position INTEGER NOT NULL, choice_id VARCHAR(60) NOT NULL, text TEXT NOT NULL,
 PRIMARY KEY(question_id,position), UNIQUE(question_id,choice_id)
);
CREATE TABLE question_keywords (
 question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
 position INTEGER NOT NULL, keyword VARCHAR(200) NOT NULL, PRIMARY KEY(question_id,position)
);
CREATE TABLE question_aliases (
 question_id UUID NOT NULL REFERENCES questions(id) ON DELETE CASCADE,
 position INTEGER NOT NULL, alias VARCHAR(500) NOT NULL, PRIMARY KEY(question_id,position)
);
