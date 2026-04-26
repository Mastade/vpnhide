_2026-04-26_

## English

kmod: stop hanging RTM_GETLINK dumps when SELinux is Permissive — replaced -EMSGSIZE return in rtnl_fill_ifinfo with the same skb_trim+return-0 trick already used for inet6_fill_ifaddr

## Русский

kmod: больше не зависает RTM_GETLINK при Permissive SELinux — в rtnl_fill_ifinfo вместо -EMSGSIZE теперь skb_trim+return 0, как уже было в inet6_fill_ifaddr
