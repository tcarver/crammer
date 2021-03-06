/*******************************************************************************
 * Copyright 2012 EMBL-EBI, Hinxton outstation
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *   http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 ******************************************************************************/
package net.sf.cram;

import net.sf.picard.util.Log;
import net.sf.picard.util.Log.LogLevel;

import com.beust.jcommander.IStringConverter;
import com.beust.jcommander.JCommander;
import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;

public class CramTools {
	public static final String CRAM2BAM_COMMAND = "bam";
	public static final String BAM2CRAM_COMMAND = "cram";

	private static Log log = Log.getInstance(CramTools.class);

	public static void main(String[] args) throws Exception {

		Params params = new Params();
		JCommander jc = new JCommander(params);
		jc.setProgramName("cramtools");

		Cram2Bam.Params cram2BamParams = new Cram2Bam.Params();
		Bam2Cram.Params bam2CramParams = new Bam2Cram.Params();

		jc.addCommand(CRAM2BAM_COMMAND, cram2BamParams);
		jc.addCommand(BAM2CRAM_COMMAND, bam2CramParams);

		jc.parse(args);

		if (params.logLevel != null) {
			Log.setGlobalLogLevel(params.logLevel);
			String[] newArgs = new String[args.length - 2];
			System.arraycopy(args, 2, newArgs, 0, newArgs.length);
			args = newArgs;
		}

		String command = jc.getParsedCommand();

		if (command == null || params.help) {
			StringBuilder sb = new StringBuilder();
			sb.append("\n");
			jc.usage(sb);

			System.out.println("Version "
					+ CramTools.class.getPackage().getImplementationVersion());
			System.out.println(sb.toString());
			return;
		}

		String[] commandArgs = new String[args.length - 1];
		System.arraycopy(args, 1, commandArgs, 0, commandArgs.length);

		if (CRAM2BAM_COMMAND.equals(command))
			Cram2Bam.main(commandArgs);
		else if (BAM2CRAM_COMMAND.equals(command))
			Bam2Cram.main(commandArgs);

	}

	@Parameters(commandDescription = "CRAM tools. ")
	private static class Params {
		@Parameter(names = { "-h", "--help" }, description = "Print help and quit")
		private boolean help = false;

		@Parameter(names = { "-l", "--log-level" }, description = "Change log level", converter = LevelConverter.class)
		private LogLevel logLevel = null;
	}

	public static class LevelConverter implements IStringConverter<LogLevel> {

		@Override
		public LogLevel convert(String s) {
			return LogLevel.valueOf(LogLevel.class, s);
		}

	}
}
