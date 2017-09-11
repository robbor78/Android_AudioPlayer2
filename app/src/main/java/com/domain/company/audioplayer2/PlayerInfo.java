package com.domain.company.audioplayer2;

import java.io.File;

public class PlayerInfo {
    public int duration = 0;
    public int position = 1;
    public String filePath;

    public String getTitle() {
        return new File(filePath).getName();
    }

    public String getSubTitle() {
        double per = 0.0;
        if (duration != 0) {
            per = 100 * (double) position / (double) duration;
        }
        int curr = position / 1000;
        int total = duration / 1000;
        return String.format("%.2f%% %d/%d", per, curr, total);
    }

    public String getCombined() {
        return getTitle() + " " + getSubTitle();
    }
}
