create table members (
    id    uuid         primary key,
    name  varchar(200) not null,
    email varchar(320) not null
);

create table book_copies (
    id    uuid         primary key,
    isbn  varchar(20)  not null,
    title varchar(500) not null
);

create table loans (
    id          uuid    primary key,
    member_id   uuid    not null references members (id),
    copy_id     uuid    not null references book_copies (id),
    borrowed_on date    not null,
    due_on      date    not null,
    renewed     boolean not null,
    returned_on date
);

-- A copy can be on loan to only one member at a time. LendingPolicy checks this too, but only the database
-- can stop two concurrent requests from both lending the same copy.
create unique index loans_one_active_per_copy on loans (copy_id) where returned_on is null;

create index loans_active_by_member on loans (member_id) where returned_on is null;
