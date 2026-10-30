# Send SMS through one provider

For whoever changes how larder sends text messages, or signs a provider contract. The worker sends every text through
one SMS provider, chosen in 2024 for a single contract and a single dashboard the volunteers already know, instead of
a second provider kept as a fallback.

## Context

In 2024 larder began sending texts from the worker, starting with shift notices to volunteers. The provider had one
outage in the first month, and Ana proposed adding a second provider that the worker would use when the first failed.
Two forces judge the options: the pantry pays for what it uses from donations, so each contract must earn its fee,
and the volunteers who run larder keep the number of accounts and dashboards they learn down.

## Options

- **Two providers, one as a fallback**: no single point of failure; lost because it doubles contracts, dashboards
  and the code that decides when to switch, for outages that so far ended within hours.
- **One provider**: chosen.

## Decision

The worker sends texts through one provider, with one contract and one dashboard. When it is down, the volunteers
resend what failed from the dashboard once it recovers, as [SMS provider down](../runbooks/sms-provider-down.md)
describes.

## Consequences

During a provider outage, texts are delayed until someone resends them. Volunteers learn one dashboard, and the
pantry pays one contract. If outages become long or frequent, a second provider can be added behind the same worker
code.
