package eu.unicore.ucc.util;

import java.nio.file.Path;
import java.util.Collection;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.jline.keymap.KeyMap;
import org.jline.reader.Binding;
import org.jline.reader.Buffer;
import org.jline.reader.EndOfFileException;
import org.jline.reader.Expander;
import org.jline.reader.Highlighter;
import org.jline.reader.History;
import org.jline.reader.LineReader;
import org.jline.reader.MaskingCallback;
import org.jline.reader.ParsedLine;
import org.jline.reader.Parser;
import org.jline.reader.UserInterruptException;
import org.jline.reader.Widget;
import org.jline.terminal.MouseEvent;
import org.jline.terminal.Terminal;
import org.jline.utils.AttributedString;

public class MockLineReader implements LineReader {

	public AtomicInteger invoked = new AtomicInteger();

	@Override
	public void addCommandsInBuffer(Collection<String> arg0) {
	}

	@Override
	public void callWidget(String arg0) {
	}

	@Override
	public Map<String, KeyMap<Binding>> defaultKeyMaps() {
		return null;
	}

	@Override
	public void editAndAddInBuffer(Path arg0) throws Exception {
	}

	@Override
	public String getAppName() {
		return null;
	}

	@Override
	public SuggestionType getAutosuggestion() {
		return null;
	}

	@Override
	public Buffer getBuffer() {
		return null;
	}

	@Override
	public Map<String, Widget> getBuiltinWidgets() {
		return null;
	}

	@Override
	public Expander getExpander() {
		return null;
	}

	@Override
	public Highlighter getHighlighter() {
		return null;
	}

	@Override
	public History getHistory() {
		return null;
	}

	@Override
	public String getKeyMap() {
		return null;
	}

	@Override
	public Map<String, KeyMap<Binding>> getKeyMaps() {
		return null;
	}

	@Override
	public KeyMap<Binding> getKeys() {
		return null;
	}

	@Override
	public String getLastBinding() {
		return null;
	}

	@Override
	public ParsedLine getParsedLine() {
		return null;
	}

	@Override
	public Parser getParser() {
		return null;
	}

	@Override
	public RegionType getRegionActive() {
		return null;
	}

	@Override
	public int getRegionMark() {
		return 0;
	}

	@Override
	public String getSearchTerm() {
		return null;
	}

	@Override
	public String getTailTip() {
		return null;
	}

	@Override
	public Terminal getTerminal() {
		return null;
	}

	@Override
	public Object getVariable(String arg0) {
		return null;
	}

	@Override
	public Map<String, Object> getVariables() {
		return null;
	}

	@Override
	public Map<String, Widget> getWidgets() {
		return null;
	}

	@Override
	public boolean isReading() {
		return false;
	}

	@Override
	public boolean isSet(Option arg0) {
		return false;
	}

	@Override
	public LineReader option(Option arg0, boolean arg1) {
		return null;
	}

	@Override
	public void printAbove(AttributedString arg0) {
	}

	@Override
	public void printAbove(String arg0) {
	}

	@Override
	public String readLine() throws UserInterruptException, EndOfFileException {
		return null;
	}

	@Override
	public String readLine(Character arg0) throws UserInterruptException, EndOfFileException {
		return null;
	}

	@Override
	public String readLine(String arg0, Character arg1, String arg2)
	        throws UserInterruptException, EndOfFileException {
		return null;
	}

	@Override
	public String readLine(String arg0, Character arg1)
	        throws UserInterruptException, EndOfFileException {
		return null;
	}

	@Override
	public String readLine(String arg0, String arg1, Character arg2, String arg3)
	        throws UserInterruptException, EndOfFileException {
		return null;
	}

	@Override
	public String readLine(String arg0, String arg1, MaskingCallback arg2, String arg3)
	        throws UserInterruptException, EndOfFileException {
		return null;
	}

	@Override
	public String readLine(String arg0) throws UserInterruptException, EndOfFileException {
		invoked.incrementAndGet();
		return "y";
	}

	@Override
	public MouseEvent readMouseEvent() {
		return null;
	}

	@Override
	public void runMacro(String arg0) {
	}

	@Override
	public void setAutosuggestion(SuggestionType arg0) {
	}

	@Override
	public boolean setKeyMap(String arg0) {
		return true;
	}

	@Override
	public void setOpt(Option arg0) {
	}

	@Override
	public void setTailTip(String arg0) {
	}

	@Override
	public void setVariable(String arg0, Object arg1) {
	}

	@Override
	public void unsetOpt(Option arg0) {
	}

	@Override
	public LineReader variable(String arg0, Object arg1) {
		return null;
	}

	@Override
	public void zeroOut() {
	}

}
