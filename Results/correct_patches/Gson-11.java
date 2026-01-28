public Number read(JsonReader in) throws IOException {
  JsonToken jsonToken = in.peek();
  switch (jsonToken) {
  case NULL:
    in.nextNull();
    return null;
  case NUMBER:
    return new LazilyParsedNumber(in.nextString());
  case STRING:
    return new LazilyParsedNumber(in.nextString()); // Added case to handle string-encoded numbers
  default:
    throw new JsonSyntaxException("Expecting number, got: " + jsonToken);
  }
}