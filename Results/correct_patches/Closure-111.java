protected JSType caseTopType(JSType topType) {
  if (topType.isAllType()) {
    return getNativeType(ARRAY_TYPE);
  }
  return topType;
}