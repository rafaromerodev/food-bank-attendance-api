-- Beneficiaries of the food bank. Never hard-deleted: deactivation keeps the
-- attendance history intact so past reports don't change.
create table beneficiary (
    id                bigint generated always as identity primary key,
    document          varchar(20)  not null unique,
    first_name        varchar(80)  not null,
    last_name         varchar(120) not null,
    phone             varchar(20)  not null,
    file_number       varchar(30),
    household_adults  integer check (household_adults >= 0),
    household_minors  integer check (household_minors >= 0),
    file_expires_on   date,
    active            boolean      not null default true,
    created_at        timestamp with time zone not null,
    updated_at        timestamp with time zone not null
);

-- NFC cards keep their own history. A lost card is deactivated, not deleted,
-- so it can be recognised if it shows up again.
create table card (
    id                   bigint generated always as identity primary key,
    uid                  varchar(32) not null,
    beneficiary_id       bigint      not null references beneficiary (id),
    assigned_at          timestamp with time zone not null,
    deactivated_at       timestamp with time zone,
    deactivation_reason  varchar(20) check (deactivation_reason in ('LOST', 'STOLEN', 'DAMAGED', 'OTHER')),
    check ((deactivated_at is null) = (deactivation_reason is null))
);

-- A UID can be active for one person only, and a person has at most one active card.
create unique index ux_card_uid_active on card (uid) where deactivated_at is null;
create unique index ux_card_beneficiary_active on card (beneficiary_id) where deactivated_at is null;
create index ix_card_uid on card (uid);

-- Attendances belong to the beneficiary, not to the card: replacing a card or
-- correcting a document never touches the history.
create table attendance (
    id              bigint generated always as identity primary key,
    beneficiary_id  bigint      not null references beneficiary (id),
    attended_on     date        not null,
    attended_at     timestamp with time zone not null,
    source          varchar(10) not null check (source in ('CARD', 'MANUAL')),
    -- One attendance per person and day, even if the card is tapped twice.
    constraint ux_attendance_once_per_day unique (beneficiary_id, attended_on)
);

create index ix_attendance_attended_on on attendance (attended_on);
