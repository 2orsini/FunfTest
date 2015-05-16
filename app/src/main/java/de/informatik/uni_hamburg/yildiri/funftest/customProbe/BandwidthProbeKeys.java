package de.informatik.uni_hamburg.yildiri.funftest.customProbe;

import edu.mit.media.funf.probe.builtin.ProbeKeys;

/**
 * These are the keys being used to assemble and pack bandwidth results into data bundles. Hence these keys also end up as the column identifiers of the database entries.
 */
public interface BandwidthProbeKeys extends ProbeKeys.BaseProbeKeys {

    public static final String URL = "url", FILE_SIZE = "file_size",
            BANDWIDTH_100 = "first_100kb", BANDWIDTH_200 = "first_200kb", BANDWIDTH_300 = "first_300kb",
            BANDWIDTH_400 = "first_400kb", BANDWIDTH_500 = "first_500kb", BANDWIDTH_600 = "first_600kb",
            BANDWIDTH_700 = "first_700kb", BANDWIDTH_800 = "first_800kb", BANDWIDTH_900 = "first_900kb",
            BANDWIDTH_1000 = "first_1000kb", BANDWIDTH_1100 = "first_1100kb", BANDWIDTH_1200 = "first_1200kb",
            BANDWIDTH_1300 = "first_1300kb", BANDWIDTH_1400 = "first_1400kb", BANDWIDTH_1500 = "first_1500kb",
            BANDWIDTH_1600 = "first_1600kb", BANDWIDTH_1700 = "first_1700kb", BANDWIDTH_1800 = "first_1800kb",
            BANDWIDTH_1900 = "first_1900kb", BANDWIDTH_2000 = "first_2000kb",
            BANDWIDTH_TOTAL = "bandwidth_total";
}
