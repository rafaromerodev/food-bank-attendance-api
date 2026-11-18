-- Staff accounts. Passwords are stored as BCrypt hashes only.
create table app_user (
    id             bigint generated always as identity primary key,
    username       varchar(60)  not null unique,
    password_hash  varchar(100) not null,
    role           varchar(20)  not null check (role in ('FRONT_DESK', 'COORDINATOR')),
    enabled        boolean      not null default true,
    created_at     timestamp with time zone not null
);
