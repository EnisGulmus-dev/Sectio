package com.enis.sectio.channel;

/** Invalidates a channel view after committed writes; no message content is broadcast. */
public record ChannelLiveEvent(Long channelId) {}
