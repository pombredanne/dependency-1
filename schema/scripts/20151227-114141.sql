alter table users alter column email drop not null;

create or replace function null_or_non_empty_trimmed_string(p_value text) returns boolean immutable cost 1 language plpgsql as $$
  begin
    if p_value is null then
      return true;
    else
      if trim(p_value) = p_value and p_value != '' then
        return true;
      else
        return false;
      end if;
    end if;
  end
$$;

alter table users drop constraint users_email_check;
alter table users add constraint users_email_check
  check (null_or_non_empty_trimmed_string(email));
