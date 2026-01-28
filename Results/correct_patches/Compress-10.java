private void resolveLocalFileHeaderData(Map<ZipArchiveEntry, NameAndComment>
                                        entriesWithoutUTF8Flag)
    throws IOException {
    // Create a list of entries to process to avoid modifying the map while iterating
    java.util.List<ZipArchiveEntry> entriesToProcess = new java.util.ArrayList<ZipArchiveEntry>(entries.keySet());
    
    for (ZipArchiveEntry ze : entriesToProcess) {
        OffsetEntry offsetEntry = entries.get(ze);
        long offset = offsetEntry.headerOffset;
        archive.seek(offset + LFH_OFFSET_FOR_FILENAME_LENGTH);
        byte[] b = new byte[SHORT];
        archive.readFully(b);
        int fileNameLen = ZipShort.getValue(b);
        archive.readFully(b);
        int extraFieldLen = ZipShort.getValue(b);
        int lenToSkip = fileNameLen;
        while (lenToSkip > 0) {
            int skipped = archive.skipBytes(lenToSkip);
            if (skipped <= 0) {
                throw new RuntimeException("failed to skip file name in"
                                           + " local file header");
            }
            lenToSkip -= skipped;
        }
        byte[] localExtraData = new byte[extraFieldLen];
        archive.readFully(localExtraData);
        ze.setExtra(localExtraData);
        offsetEntry.dataOffset = offset + LFH_OFFSET_FOR_FILENAME_LENGTH
            + SHORT + SHORT + fileNameLen + extraFieldLen;

        if (entriesWithoutUTF8Flag.containsKey(ze)) {
            String orig = ze.getName();
            NameAndComment nc = entriesWithoutUTF8Flag.get(ze);
            ZipUtil.setNameAndCommentFromExtraFields(ze, nc.name,
                                                     nc.comment);
            if (!orig.equals(ze.getName())) {
                // Update both maps when name changes
                entries.remove(ze);
                nameMap.remove(orig);
                nameMap.put(ze.getName(), ze);
                entries.put(ze, offsetEntry);
            }
        }
    }
}