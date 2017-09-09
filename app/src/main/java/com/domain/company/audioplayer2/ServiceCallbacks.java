package com.domain.company.audioplayer2;

public interface ServiceCallbacks {
    void info(PlayerInfo pi);

    void seekComplete();

    void stopped();

    void playing();

    void paused();

    void unpaused();
}