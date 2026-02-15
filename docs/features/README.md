# AI Feature Docs

This directory contains one Discord-compatible markdown file per dense AI feature used by the local model, plus dedicated token feature family docs written as textbook-style explanations.

## Dense Feature Files
- [behavior_hits_norm](./behavior_hits_norm.md)
- [channel_party](./channel_party.md)
- [channel_pm](./channel_pm.md)
- [channel_public](./channel_public.md)
- [ctx_advertising](./ctx_advertising.md)
- [ctx_asks_for_stuff](./ctx_asks_for_stuff.md)
- [ctx_claims_middleman_without_proof](./ctx_claims_middleman_without_proof.md)
- [ctx_demands_upfront_payment](./ctx_demands_upfront_payment.md)
- [ctx_is_spam](./ctx_is_spam.md)
- [ctx_pushes_external_platform](./ctx_pushes_external_platform.md)
- [ctx_repeated_contact_3plus](./ctx_repeated_contact_3plus.md)
- [ctx_requests_sensitive_data](./ctx_requests_sensitive_data.md)
- [ctx_too_good_to_be_true](./ctx_too_good_to_be_true.md)
- [funnel_full_chain](./funnel_full_chain.md)
- [funnel_hits_norm](./funnel_hits_norm.md)
- [funnel_partial_chain](./funnel_partial_chain.md)
- [funnel_sequence_norm](./funnel_sequence_norm.md)
- [funnel_step_norm](./funnel_step_norm.md)
- [has_link](./has_link.md)
- [has_suspicious_punctuation](./has_suspicious_punctuation.md)
- [intent_anchor](./intent_anchor.md)
- [intent_instruction](./intent_instruction.md)
- [intent_offer](./intent_offer.md)
- [intent_payment](./intent_payment.md)
- [intent_redirect](./intent_redirect.md)
- [intent_rep](./intent_rep.md)
- [kw_account](./kw_account.md)
- [kw_payment](./kw_payment.md)
- [kw_platform](./kw_platform.md)
- [kw_too_good](./kw_too_good.md)
- [kw_trust](./kw_trust.md)
- [kw_urgency](./kw_urgency.md)
- [rapid_followup](./rapid_followup.md)
- [rule_hits_norm](./rule_hits_norm.md)
- [similarity_hits_norm](./similarity_hits_norm.md)
- [trend_hits_norm](./trend_hits_norm.md)

## Token Feature Files
- [Token Feature Index](./tokens/README.md)
- [Token Feature Overview](./tokens/token_feature_overview.md)
- [Feature Family ng2](./tokens/ng2.md)
- [Feature Family ng3](./tokens/ng3.md)
- [Feature Family ng4](./tokens/ng4.md)
- [Feature Family ng5](./tokens/ng5.md)
- [Vocabulary Selection and Retention](./tokens/vocabulary_selection.md)

## Notes
- Dense features are either binary indicators or normalized numeric values.
- Token features are dynamic phrase-level keys derived from message n-grams.
- The funnel head consumes only a subset of dense features and no token features.
