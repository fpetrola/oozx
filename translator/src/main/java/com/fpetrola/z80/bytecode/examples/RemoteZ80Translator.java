package com.fpetrola.z80.bytecode.examples;

import com.fpetrola.z80.bytecode.RealCodeBytecodeCreationBase;
import com.fpetrola.z80.jspeccy.MemorySetter;
import com.fpetrola.z80.jspeccy.SnapshotLoader;
import com.fpetrola.z80.opcodes.references.WordNumber;
import com.fpetrola.z80.routines.Routine;
import io.korhner.asciimg.image.AsciiImgCache;
import io.korhner.asciimg.image.character_fit_strategy.StructuralSimilarityFitStrategy;
import io.korhner.asciimg.image.converter.AsciiToStringConverter;
import org.apache.commons.text.CaseUtils;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;

import static java.net.URI.create;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;

public class RemoteZ80Translator<T extends WordNumber> extends RealCodeBytecodeCreationBase<T> {
  public static void main(String[] args) {
    RemoteZ80Translator<WordNumber> remoteZ80Translator = new RemoteZ80Translator<>();

    String action = "translate";
    String gameName = "jetsetwilly";
    String url = "http://torinak.com/qaop/bin/" + gameName;
    int startRoutineAddress = 34762;
    String screenURL = "https://tcrf.net/images/3/3a/Jet_Set_Willy-ZX_Spectrum-title.png";


    if (args.length >= 4) {
      action = args[0];
      gameName = args[1];
      url = args[2];
      startRoutineAddress = Integer.parseInt(args[3]);
      if (args.length > 4)
        screenURL = args[4];
    }

    System.out.println("\n\nTranslating: " + gameName + " " + url + " " + startRoutineAddress + "\n\n");

    remoteZ80Translator.translate(action, gameName, url, startRoutineAddress, screenURL);
  }

  private void drawPicture(String url) {
    try {
      File input = getRemoteFile(url, "", "/tmp/" + "screen");

      AsciiImgCache cache = AsciiImgCache.create(new Font("Courier", Font.PLAIN, 2));
      BufferedImage portraitImage = ImageIO.read(input);
      AsciiToStringConverter stringConverter = new AsciiToStringConverter(cache, new StructuralSimilarityFitStrategy());
      System.out.println(stringConverter.convertImage(portraitImage));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  public void translate(String action, String gameName, String url, int startRoutineAddress, String screeenURL) {
    //  drawPicture(screeenURL);

    File tempFile = getRemoteFile(url, ".z80", "/tmp/" + gameName + ".z80");


    SnapshotLoader.setupStateWithSnapshot(gettDefaultRegistersSetter(), tempFile.getAbsolutePath(), new MemorySetter(state.getMemory()));

    int firstAddress = state.getPc().read().intValue();
    String base64Memory = SnapshotHelper.getBase64Memory(state);
    stepUntilComplete(firstAddress);

    List<Routine> routines = getRoutines();
    String className = CaseUtils.toCamelCase(gameName, true);

    if (action.equals("translate")) {
      String targetFolder = "target/translation/";
      String sourceCode = generateAndDecompile(base64Memory, routines, targetFolder, className);

      try {
        String fileName = className + ".java";
        FileWriter fileWriter = new FileWriter(targetFolder + fileName);
        fileWriter.write(improveSource(sourceCode));
        fileWriter.close();
        System.out.println("\n\nWritting java source code to: " + fileName + "\n\n");
      } catch (IOException e) {
        throw new RuntimeException(e);
      }
    } else
      translateToJava(gameName, base64Memory, "$" + startRoutineAddress, routines);
  }

  public static String improveSource(String sourceCode) {
    sourceCode = sourceCode.replace("this.", "").replace("super.", "");
    sourceCode = sourceCode.replaceAll("\\(\\(.*\\)this\\).", "");
    sourceCode = StringReplacer.replace(sourceCode, Pattern.compile("('\\\\u([0-9a-f]{4})')"), m -> {
      String group = m.group(2);
      return String.valueOf(Integer.parseInt(group, 16));
    });
    return sourceCode;
  }

  private File getRemoteFile(String url, String suffix, String pathname) {
    try {
      File tempFile = new File(pathname);
      // File tempFile = File.createTempFile("zx-" + gameName + "-", suffix);
      Path target = tempFile.toPath();
      Files.copy(create(url).toURL().openStream(), target, REPLACE_EXISTING);
      return tempFile;
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }
}