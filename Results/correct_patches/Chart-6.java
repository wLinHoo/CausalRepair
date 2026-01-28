public boolean equals(Object obj) {
    if (obj == this) {
        return true;
    }
    if (!(obj instanceof ShapeList)) {
        return false;
    }
    ShapeList that = (ShapeList) obj;
    if (this.size() != that.size()) {
        return false;
    }
    for (int i = 0; i < this.size(); i++) {
        Shape s1 = (Shape) this.get(i);
        Shape s2 = (Shape) that.get(i);
        if (!ShapeUtilities.equal(s1, s2)) {
            return false;
        }
    }
    return true;
}