_2026-04-27_

## English

lsposed: install-time smoke-check on private AOSP fields used by writeToParcel hooks. If AOSP renames or retypes mIfaceName / mTransportTypes / mNetworkType / similar, the affected hook is skipped at install (no per-call NoSuchFieldError spam) and the dashboard shows a red error listing the broken fields and Android SDK so users can file a bug.

## Русский

lsposed: smoke-check приватных AOSP-полей при установке writeToParcel-хуков. Если в новой AOSP поле переименовано или сменило тип, соответствующий хук пропускается при установке (без спама NoSuchFieldError на каждом вызове), а дашборд показывает красную ошибку со списком сломанных полей и версией Android — чтобы пользователь мог завести issue.
