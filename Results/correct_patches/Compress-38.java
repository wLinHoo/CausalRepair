public boolean isDirectory() {
    if (file != null) {
        return file.isDirectory();
    }

    if (linkFlag == LF_DIR) {
        return true;
    }

    String name = getName();
    if (name.endsWith("/") && !name.startsWith("PaxHeader")) {
        return true;
    }

    return false;
}