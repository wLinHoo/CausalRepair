void read(Tokeniser t, CharacterReader r) {
    char c = r.consume();
    switch (c) {
        case '>':
            t.tagPending.selfClosing = true;
            t.emitTagPending();
            t.transition(Data);
            break;
        case eof:
            t.eofError(this);
            t.transition(Data);
            break;
        default:
            t.error(this);
            r.unconsume(); // Add the '/' back to be included in attribute name
            t.transition(BeforeAttributeName);
    }
}