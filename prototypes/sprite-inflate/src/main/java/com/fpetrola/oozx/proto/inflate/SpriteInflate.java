package com.fpetrola.oozx.proto.inflate;

import io.github.stanio.xbrz.Xbrz;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

/**
 * A sprite, given a body: xBRZ for the outline, a distance field for the bulge, a mesh out the
 * other end.
 * <p>
 * The pipeline is five steps and each one exists for a reason worth stating:
 * <ol>
 *   <li><b>xBRZ 4x.</b> What reads as jagged once a sprite is standing up in 3D is its outline,
 *       and this is the thing that fixes outlines without inventing colours.</li>
 *   <li><b>Coverage, not a mask.</b> xBRZ leaves blended pixels along every edge, and that blend
 *       IS the sub-pixel information the 4x bought. Thresholding it back to hard pixels throws
 *       that away and leaves stair-steps four times smaller, which is not the same as smooth.
 *       So the alpha is kept as fractional coverage and the outline is taken at coverage 0.5.</li>
 *   <li><b>A distance field</b> inside that outline, exact (Felzenszwalb's transform), telling
 *       every point how far it is from the edge.</li>
 *   <li><b>Local thickness</b> - the radius of the largest inscribed disc that covers a point,
 *       which is what tells a two-pixel leg it is a two-pixel leg and not part of the torso.</li>
 *   <li><b>The bulge</b>, which is where the profiles below differ, mirrored: the same amount
 *       forwards as backwards, so a section across a limb is a whole ellipse. The two halves meet
 *       at the outline, where the depth is zero, so the solid closes without a wall around the
 *       rim - and the figure is symmetric, which is what it should be if it is to be turned all
 *       the way round rather than looked at from the front.</li>
 * </ol>
 */
public class SpriteInflate {

  /**
   * Three ways to turn "how far from the edge" into "how far back", which is the whole question.
   * <p>
   * They are all here because the difference only becomes obvious on a figure with thin parts,
   * and a sprite of a person is nothing but thin parts.
   */
  enum Profile {
    /**
     * Depth proportional to distance from the edge: the original inflation, and the one that
     * makes a person look like a gingerbread man. A torso six pixels wide bulges three; an arm
     * two pixels wide bulges one, which against that torso is flat.
     */
    LINEAR("linear") {
      double depth(double distance, double localRadius, double largestRadius) {
        return distance;
      }
    },
    /**
     * The height of a sphere of the WHOLE figure's radius. Fixes the flatness and overshoots:
     * two pixels from the edge of a six-pixel torso is 2.2 deep, so a two-pixel arm comes out a
     * blade standing on edge, deeper than it is wide.
     */
    SPHERE_GLOBAL("sphere-global") {
      double depth(double distance, double localRadius, double largestRadius) {
        double d = Math.min(distance, largestRadius);
        return Math.sqrt(d * (2 * largestRadius - d));
      }
    },
    /**
     * The height of a sphere of the radius that fits HERE. An arm two pixels wide gets a round
     * section one pixel deep, the torso gets one three pixels deep, and each part is as round as
     * it is wide - which is what an inflated drawing of a person should be.
     */
    SPHERE_LOCAL("sphere-local") {
      double depth(double distance, double localRadius, double largestRadius) {
        double d = Math.min(distance, localRadius);
        return Math.sqrt(d * (2 * localRadius - d));
      }
    },
    /**
     * A dome that leaves the outline at an angle instead of straight up.
     * <p>
     * The sphere is right about the shape and awkward about the rim: its slope there is vertical
     * - the derivative of the square root goes to infinity as the distance goes to zero - so on a
     * grid the first ring of faces around the whole silhouette stands almost on end, and catches
     * the light as a comb of facets along every edge. This leaves the rim at a slope of pi over
     * two instead, which a grid can represent.
     * <p>
     * IT DID NOT WORK, and is kept for the next person who has the idea. The comb along the rim
     * goes, and a crease appears down the middle of every arm and leg instead: nearer the middle
     * this profile is nearly straight, so it meets itself from both sides at an angle rather than
     * rounding over, which is the same ridge {@link #LINEAR} has and the thing the sphere was
     * chosen to avoid. Trading a rough rim for a creased middle is a bad trade - the rim is one
     * ring of faces and the crease runs the length of the figure. If the rim really matters,
     * the answer is more samples across it, not a flatter dome.
     */
    DOME_LOCAL("dome-local") {
      double depth(double distance, double localRadius, double largestRadius) {
        double d = Math.min(distance, localRadius);
        return localRadius * Math.sin(Math.PI / 2 * d / Math.max(localRadius, 1e-9));
      }
    };

    final String label;

    Profile(String label) {
      this.label = label;
    }

    abstract double depth(double distance, double localRadius, double largestRadius);
  }

  /**
   * A stand-in figure, NOT the real Willy - drawn here in his proportions (sixteen high, a head,
   * a torso, two-pixel arms and legs) so the thin parts the profiles disagree about are present.
   * Feed a PNG on the command line to run the same pipeline on the real thing.
   */
  private static final String[] SAMPLE = {
      "......oooo......",
      ".....oooooo.....",
      ".....oo..oo.....",
      ".....oooooo.....",
      "......oooo......",
      "....########....",
      "..#.########.#..",
      "..#.########.#..",
      "..#.########.#..",
      "..############..",
      "....########....",
      ".....######.....",
      ".....##..##.....",
      ".....##..##.....",
      "....###..###....",
      "...####..####...",
  };

  private static final int SKIN = 0xFFE8B080;
  private static final int BODY = 0xFF3050C0;

  public static void main(String[] args) throws Exception {
    Path out = Path.of(args.length > 1 ? args[1] : "out");
    Files.createDirectories(out);

    BufferedImage sprite = args.length > 0 ? ImageIO.read(new File(args[0])) : sample();
    System.out.printf("sprite: %dx%d%n", sprite.getWidth(), sprite.getHeight());

    // "4", or "4x2" for eight times in two passes: xBRZ itself refuses anything but two to six.
    int[] passes = passes(args.length > 2 ? args[2] : "4");
    // The fourth argument is the seam rounding, because comparing it against itself is the only
    // way to see what it does.
    double rimRoll = args.length > 3 ? Double.parseDouble(args[3]) : 0.6;
    int relaxing = args.length > 4 ? Integer.parseInt(args[4]) : 4;
    Options options = new Options(passes, 1.0, 3, 0.45, 0.4, 2.0, true, rimRoll, relaxing);
    // The same call the viewer makes. It used to be a second copy of it here, and the two drifted
    // apart at once: the viewer learned to paint the filled-in detail and the pictures did not.
    Fields fields = measure(sprite, options, true);
    int width = fields.width(), height = fields.height();
    double[] coverage = fields.coverage();
    double[] thickness = fields.thickness();
    double largest = fields.largest();

    ImageIO.write(fields.scaled(), "png", out.resolve("1-xbrz-4x.png").toFile());
    ImageIO.write(nearest(padded(sprite, 2, false), fields.factor()), "png",
        out.resolve("1-nearest-4x.png").toFile());
    writeGrey(fields.distance(), width, height, largest, out.resolve("2-distance.png"));
    writeGrey(thickness, width, height, largest, out.resolve("3-thickness.png"));
    writeContour(coverage, width, height, out.resolve("4-outline.svg"));

    List<BufferedImage> rows = new ArrayList<>();
    for (Profile profile : Profile.values()) {
      double[] depth = depths(fields, profile, options);
      double deepest = 0;
      for (int i = 0; i < depth.length; i++) {
        deepest = Math.max(deepest, depth[i]);
      }
      List<double[]> rough = build(coverage, depth, width, height, options.mirrored());
      List<double[]> mesh = relaxed(rough, options.relaxing());
      System.out.printf("  %-14s neighbouring faces disagree by %.2f degrees, %.2f after "
              + "%d passes of smoothing%n",
          profile.label, roughness(rough), roughness(mesh), options.relaxing());
      writeObj(mesh, out.resolve("5-" + profile.label + ".obj"));
      // The maximum is the same point for all three - the middle of the biggest disc - so it
      // says nothing. What separates them is what happens where the figure is thin.
      double thinDepth = 0, thinWidth = 0, thickDepth = 0, thickWidth = 0;
      int thinCount = 0, thickCount = 0;
      for (int i = 0; i < depth.length; i++) {
        if (coverage[i] < 0.5) continue;
        if (thickness[i] < largest * 0.3) {
          thinDepth += depth[i];
          thinWidth += thickness[i];
          thinCount++;
        } else if (thickness[i] > largest * 0.8) {
          thickDepth += depth[i];
          thickWidth += thickness[i];
          thickCount++;
        }
      }
      System.out.printf("%-14s deepest %5.2f   thin parts: %4.2f deep in %4.2f of width "
              + "(%.2fx)   thick: %5.2f deep in %5.2f (%.2fx)%n",
          profile.label, deepest,
          thinDepth / thinCount, thinWidth / thinCount, thinDepth / thinWidth,
          thickDepth / thickCount, thickWidth / thickCount, thickDepth / thickWidth);
      rows.add(turntable(mesh, fields, profile.label));
    }
    ImageIO.write(stack(rows), "png", out.resolve("6-profiles.png").toFile());

    // The claim this whole ordering rests on: that scaling first is only worth it if the blend
    // it leaves is kept as coverage. Three silhouettes, same bulge, and the outline is the only
    // thing that differs between them.
    double[] hard = coverageOf(nearest(padded(sprite, 2, false), fields.factor()));
    double[] thresholded = new double[coverage.length];
    for (int i = 0; i < coverage.length; i++) {
      thresholded[i] = coverage[i] >= 0.5 ? 1 : 0;
    }
    List<BufferedImage> outlines = new ArrayList<>();
    outlines.add(inflated("no scaling, hard pixels", hard, fields));
    outlines.add(inflated("xBRZ then thresholded", thresholded, fields));
    outlines.add(inflated("xBRZ kept as coverage", coverage, fields));
    ImageIO.write(stack(outlines), "png", out.resolve("7-outline.png").toFile());

    System.out.println();
    System.out.println("length of the outline against the square root of its area "
        + "(a staircase is about 40% longer than the line it climbs; lower is smoother)");
    System.out.printf("  no scaling, hard pixels  %.3f%n", outlineLength(hard, width, height));
    System.out.printf("  xBRZ then thresholded    %.3f%n", outlineLength(thresholded, width, height));
    System.out.printf("  xBRZ kept as coverage    %.3f%n", outlineLength(coverage, width, height));
    // A control, because the figure above is nearly all upright and level edges and so has
    // little for a scaler to round. A disc is nothing but diagonal, and shows what the step from
    // thresholding to coverage is worth where there is actually something to smooth.
    BufferedImage disc = disc(16);
    BufferedImage discBig = disc;
    // The same passes, not the product: xBRZ takes two to six and eight is reached by chaining.
    for (int pass : options.passes()) {
      discBig = xbrz(discBig, pass);
    }
    double[] discCoverage = coverageOf(discBig);
    double[] discHard = coverageOf(nearest(disc, fields.factor()));
    double[] discThresholded = new double[discCoverage.length];
    for (int i = 0; i < discCoverage.length; i++) {
      discThresholded[i] = discCoverage[i] >= 0.5 ? 1 : 0;
    }
    int dw = discBig.getWidth(), dh = discBig.getHeight();
    System.out.println();
    System.out.println("the same three on a disc, which is all diagonal");
    System.out.printf("  no scaling, hard pixels  %.3f%n", outlineLength(discHard, dw, dh));
    System.out.printf("  xBRZ then thresholded    %.3f%n", outlineLength(discThresholded, dw, dh));
    System.out.printf("  xBRZ kept as coverage    %.3f%n", outlineLength(discCoverage, dw, dh));
    System.out.printf("  a true circle would be   %.3f%n", 2 * Math.sqrt(Math.PI));

    System.out.println("written to " + out.toAbsolutePath());
  }

  /**
   * Every number in this that is a judgement rather than a fact.
   * <p>
   * They were constants in the code, which is the wrong place for them: not one can be settled by
   * reasoning - a dent of 0.45 against 0.6 is a question about what an eye looks like - and every
   * one of them was chosen by rendering it once and squinting. Out here they can be turned while
   * looking at the thing they change.
   *
   * @param passes     how xBRZ is applied: {4} for four times, {4,2} for eight in two goes
   * @param depth      the bulge, as a multiple of what the profile says
   * @param smoothing  how hard the local thickness is smoothed; it comes out of a maximum over a
   *                   discrete set and so arrives in steps, and every step is a terrace
   * @param dentDepth  how far a filled-in hole is pressed back in, as a fraction of the depth
   * @param dentReach  how wide that dent is, in pixels OF THE ORIGINAL SPRITE
   * @param holeAcross how big a hole may be, in pixels of the original sprite, and still count as
   *                   detail to be filled rather than shape to be kept
   * @param mirrored   the same bulge front and back, rather than a flat front
   * @param rimRoll    over how many pixels the surface is allowed to turn over at the outline;
   *                   zero leaves the profile exactly as it is
   * @param relaxing   how many passes of smoothing are run over the finished mesh, which is the
   *                   only step that can touch the outline itself
   */
  record Options(int[] passes, double depth, int smoothing, double dentDepth, double dentReach,
                 double holeAcross, boolean mirrored, double rimRoll, int relaxing) {

    static Options standard() {
      return new Options(new int[]{4}, 1.0, 3, 0.45, 0.4, 2.0, true, 0.6, 4);
    }

    int factor() {
      int factor = 1;
      for (int pass : passes) {
        factor *= pass;
      }
      return factor;
    }
  }

  /** Everything the shape of a sprite decides, before any one profile is chosen. */
  record Fields(BufferedImage scaled, double[] coverage, double[] distance, double[] thickness,
                double largest, int width, int height, int factor,
                boolean[] detail, double[] fromDetail) {
  }

  /**
   * The whole of the measuring, for a caller that wants a solid and not a directory of files.
   * <p>
   * It is the same code the pictures come from, called quietly. Fast enough to run while someone
   * is holding a key down: everything here is linear in the pixels except the local thickness,
   * which is quadratic and still nothing at a sprite's size.
   */
  static Fields measure(BufferedImage sprite, Options options) {
    return measure(sprite, options, false);
  }

  static Fields measure(BufferedImage sprite, Options options, boolean talk) {
    BufferedImage big = padded(sprite, 2, talk);
    int factor = 1;
    for (int pass : options.passes()) {
      big = xbrz(big, pass);
      factor *= pass;
    }
    if (talk) {
      System.out.printf("scaled %s = %dx%n", java.util.Arrays.toString(options.passes())
          .replaceAll("[\\[\\] ]", "").replace(',', 'x'), factor);
    }
    int width = big.getWidth(), height = big.getHeight();
    double[] coverage = coverageOf(big);
    boolean[] detail =
        fillSmallHoles(coverage, width, height, options.holeAcross() * factor, talk);
    double[] fromDetail = distanceFrom(detail, width, height);
    double[] distance = distanceInside(coverage, width, height);
    double[] thickness = localThickness(distance, width, height);
    soften(thickness, coverage, width, height, options.smoothing());
    double largest = 0;
    for (int i = 0; i < thickness.length; i++) {
      thickness[i] = Math.max(thickness[i], distance[i]);
      largest = Math.max(largest, distance[i]);
    }
    if (talk) {
      System.out.printf("largest inscribed radius: %.2f px (at %dx)%n", largest, factor);
    }
    return new Fields(big, coverage, distance, thickness, largest, width, height, factor,
        detail, fromDetail);
  }

  /** A sprite as a closed solid, ready to be handed to a renderer. */
  static List<double[]> inflate(Fields fields, Profile profile, Options options) {
    return relaxed(build(fields.coverage(), depths(fields, profile, options), fields.width(),
        fields.height(), options.mirrored()), options.relaxing());
  }

  /**
   * Smooths the finished mesh, which is the only step that can do anything about the outline.
   * <p>
   * Everything before this works on a grid, and the sawtooth left along the edges of the figure
   * is that grid: xBRZ leaves most edges hard, so the half-coverage line they are cut on runs in
   * steps of one output pixel, and where the surface turns over quickly each of those steps
   * becomes a facet with its own idea of where the light is. More resolution does not help - the
   * scaler decides which corners to round on the ORIGINAL grid and only draws those same
   * decisions more finely. Once it is a mesh, though, the steps are just vertices in the wrong
   * place, and moving each towards the middle of its neighbours puts them right.
   * <p>
   * Taubin's, not plain Laplacian: averaging alone shrinks a solid a little on every pass, and
   * after a dozen the figure has visibly lost weight. The second pass of each pair pushes back
   * out by slightly more than the first pulled in, which cancels the shrinking while leaving the
   * smoothing - the two coefficients here are the usual 0.5 and -0.53.
   * <p>
   * Topology is untouched, so a closed solid stays closed: this only moves vertices.
   */
  static List<double[]> relaxed(List<double[]> triangles, int passes) {
    if (passes <= 0 || triangles.isEmpty()) {
      return triangles;
    }
    java.util.Map<String, Integer> numbered = new java.util.LinkedHashMap<>();
    List<double[]> points = new ArrayList<>();
    int[][] faces = new int[triangles.size()][3];
    for (int t = 0; t < triangles.size(); t++) {
      double[] triangle = triangles.get(t);
      for (int i = 0; i < 3; i++) {
        double x = triangle[i * 3], y = triangle[i * 3 + 1], z = triangle[i * 3 + 2];
        String at = key(x, y, z);
        Integer already = numbered.get(at);
        if (already == null) {
          already = points.size();
          numbered.put(at, already);
          points.add(new double[]{x, y, z});
        }
        faces[t][i] = already;
      }
    }

    List<java.util.Set<Integer>> neighbours = new ArrayList<>();
    for (int i = 0; i < points.size(); i++) {
      neighbours.add(new java.util.HashSet<>());
    }
    for (int[] face : faces) {
      for (int i = 0; i < 3; i++) {
        neighbours.get(face[i]).add(face[(i + 1) % 3]);
        neighbours.get(face[(i + 1) % 3]).add(face[i]);
      }
    }

    double[][] at = points.toArray(new double[0][]);
    for (int pass = 0; pass < passes; pass++) {
      at = towardsNeighbours(at, neighbours, 0.5);
      at = towardsNeighbours(at, neighbours, -0.53);
    }

    List<double[]> out = new ArrayList<>(triangles.size());
    for (int[] face : faces) {
      out.add(new double[]{
          at[face[0]][0], at[face[0]][1], at[face[0]][2],
          at[face[1]][0], at[face[1]][1], at[face[1]][2],
          at[face[2]][0], at[face[2]][1], at[face[2]][2]});
    }
    return out;
  }

  /** One pass: every vertex a step of {@code weight} towards the average of its neighbours. */
  private static double[][] towardsNeighbours(double[][] at, List<java.util.Set<Integer>> neighbours,
                                              double weight) {
    double[][] moved = new double[at.length][3];
    for (int i = 0; i < at.length; i++) {
      java.util.Set<Integer> around = neighbours.get(i);
      if (around.isEmpty()) {
        moved[i] = at[i].clone();
        continue;
      }
      double x = 0, y = 0, z = 0;
      for (int other : around) {
        x += at[other][0];
        y += at[other][1];
        z += at[other][2];
      }
      int count = around.size();
      moved[i][0] = at[i][0] + weight * (x / count - at[i][0]);
      moved[i][1] = at[i][1] + weight * (y / count - at[i][1]);
      moved[i][2] = at[i][2] + weight * (z / count - at[i][2]);
    }
    return moved;
  }

  /**
   * How much the surface disagrees with itself from one triangle to the next, in degrees.
   * <p>
   * A sawtooth is exactly this: neighbouring faces pointing in visibly different directions. It
   * is what the eye is reacting to, so it is the thing to measure rather than the thing to argue
   * about.
   */
  static double roughness(List<double[]> triangles) {
    java.util.Map<Long, double[]> byEdge = new java.util.HashMap<>();
    java.util.Map<String, Integer> numbered = new java.util.LinkedHashMap<>();
    double total = 0;
    int counted = 0;
    for (double[] triangle : triangles) {
      int[] face = new int[3];
      for (int i = 0; i < 3; i++) {
        face[i] = numbered.computeIfAbsent(
            key(triangle[i * 3], triangle[i * 3 + 1], triangle[i * 3 + 2]), unused -> numbered.size());
      }
      double[] normal = cross(
          new double[]{triangle[0], triangle[1], triangle[2]},
          new double[]{triangle[3], triangle[4], triangle[5]},
          new double[]{triangle[6], triangle[7], triangle[8]});
      double length = Math.sqrt(
          normal[0] * normal[0] + normal[1] * normal[1] + normal[2] * normal[2]);
      if (length < 1e-12) {
        continue;
      }
      for (int i = 0; i < 3; i++) {
        long edge = (long) Math.min(face[i], face[(i + 1) % 3]) << 32
            | Math.max(face[i], face[(i + 1) % 3]);
        double[] other = byEdge.get(edge);
        if (other == null) {
          byEdge.put(edge, new double[]{normal[0] / length, normal[1] / length, normal[2] / length});
        } else {
          double dot = other[0] * normal[0] / length + other[1] * normal[1] / length
              + other[2] * normal[2] / length;
          total += Math.toDegrees(Math.acos(Math.max(-1, Math.min(1, dot))));
          counted++;
        }
      }
    }
    return counted == 0 ? 0 : total / counted;
  }

  /** How far back the surface stands at every point, which is the whole of a profile's opinion. */
  static double[] depths(Fields fields, Profile profile, Options options) {
    double[] depth = new double[fields.width() * fields.height()];
    int factor = fields.factor();
    // A hole that was filled in was drawn for a reason - it is an eye, a button, a spot - and
    // filling it silently throws that away. It cannot come back as a hole: as a hole it bores a
    // tunnel through the solid and, worse, the distance field measures away from it and leaves
    // the whole face around it thin. So it comes back as a dent, which is what an eye is anyway,
    // and as a colour. The dent is shallow on purpose: deep enough to catch the light along one
    // side, never deep enough to meet the other surface and re-open the hole it was filling.
    double reach = Math.max(1e-6, options.dentReach() * fields.factor());
    for (int i = 0; i < depth.length; i++) {
      if (fields.coverage()[i] >= 0.5) {
        depth[i] = profile.depth(fields.distance()[i], fields.thickness()[i], fields.largest())
            * options.depth();
        double away = fields.fromDetail()[i] / reach;
        depth[i] *= 1 - options.dentDepth() * Math.exp(-away * away);
        depth[i] = rolled(depth[i], fields.distance()[i], fields.thickness()[i], options, factor);
      }
    }
    return depth;
  }

  /**
   * Spreads the turn at the outline over something the grid can actually draw.
   * <p>
   * The seam where the front of the solid meets the back is not a crease - with the sphere
   * profile the section there is a parabola, which is to say the equator of an ellipsoid, and
   * perfectly smooth. It LOOKS like a crease because the whole hundred and eighty degrees of it
   * happens inside one pixel: half a pixel in from the edge of a figure ten across, the surface
   * has already risen three units. Two samples across a half-turn shade as an edge no matter how
   * smooth the thing being sampled.
   * <p>
   * So the surface is not allowed to rise faster than a given slope near the rim, and the slope
   * is chosen per point so that the straight part gives out after {@code rimRoll} pixels: a cone
   * of slope s leaves the sphere of radius R at a distance of 2R/s squared, so s is the root of
   * 2R over the width wanted. Combined with the profile as a b over the root of a squared plus b
   * squared, which is the smooth version of taking the smaller of the two - a plain minimum would
   * put a crease exactly where this is trying to take one out.
   */
  private static double rolled(double depth, double distance, double radius, Options options,
                               int factor) {
    double width = options.rimRoll() * factor;
    if (width <= 0 || depth <= 0 || radius <= 0) {
      return depth;
    }
    double slope = Math.sqrt(2 * radius / width);
    double cone = slope * distance;
    return depth * cone / Math.sqrt(depth * depth + cone * cone);
  }

  /**
   * The colour of the surface over a point of the sprite.
   * <p>
   * Three cases, and the middle one is the reason this exists. A point over the drawing takes the
   * drawing's colour. A point over a piece of filled-in detail takes a darkened version of what
   * surrounds it, so Willy keeps an eye instead of a blank white face. And a point on the very
   * outline sits half in the background, where there is no colour at all, so the nearest thing
   * that has one is taken - without that the rim of every figure comes out black.
   */
  static int colourFor(Fields fields, double x, double y) {
    int cx = Math.min(fields.width() - 1, Math.max(0, (int) Math.round(x)));
    int cy = Math.min(fields.height() - 1, Math.max(0, (int) Math.round(y)));
    boolean detail = fields.detail()[cy * fields.width() + cx];
    for (int radius = 0; radius <= 4; radius++) {
      for (int dy = -radius; dy <= radius; dy++) {
        for (int dx = -radius; dx <= radius; dx++) {
          int sx = cx + dx, sy = cy + dy;
          if (sx < 0 || sy < 0 || sx >= fields.width() || sy >= fields.height()) continue;
          if (detail && fields.detail()[sy * fields.width() + sx]) continue;
          int argb = fields.scaled().getRGB(sx, sy);
          if ((argb >>> 24) >= 128) {
            return detail ? darken(argb & 0xFFFFFF) : argb & 0xFFFFFF;
          }
        }
      }
    }
    return detail ? 0x201810 : 0xD0D0D0;
  }

  private static int darken(int rgb) {
    return ((int) (((rgb >> 16) & 0xFF) * 0.18) << 16)
        | ((int) (((rgb >> 8) & 0xFF) * 0.18) << 8) | (int) ((rgb & 0xFF) * 0.18);
  }

  /** One silhouette, inflated the good way, for comparing outlines against each other. */
  private static BufferedImage inflated(String label, double[] coverage, Fields fields) {
    int width = fields.width(), height = fields.height();
    double[] distance = distanceInside(coverage, width, height);
    double[] thickness = localThickness(distance, width, height);
    double largest = 0;
    for (double one : distance) largest = Math.max(largest, one);
    double[] depth = new double[width * height];
    for (int i = 0; i < depth.length; i++) {
      if (coverage[i] >= 0.5) {
        depth[i] = Profile.SPHERE_LOCAL.depth(distance[i], thickness[i], largest);
      }
    }
    return turntable(build(coverage, depth, width, height, true), fields, label);
  }

  /**
   * The length of the outline, which is what says whether it is a staircase or a line.
   * <p>
   * No walk around it is needed and none is wanted: a staircase climbing a diagonal is two units
   * long for every unit of diagonal where the line itself is the square root of two, so it comes
   * out about forty per cent longer whatever the size of the steps. That is the whole difference
   * between rounding a scaled sprite back off to whole pixels and keeping what it computed.
   * <p>
   * Divided by the square root of the area so it does not simply reward a smaller figure. The
   * first attempt at this measured the angle between consecutive segments and reported the
   * staircase as the SMOOTHEST of the three - the segments were being taken in raster order, so
   * it was comparing a piece of one arm with a piece of the other, and an axis-aligned staircase
   * wins a contest of that sort because its segments are all parallel.
   */
  private static double outlineLength(double[] coverage, int width, int height) {
    double length = 0, area = 0;
    for (int y = 0; y + 1 < height; y++) {
      for (int x = 0; x + 1 < width; x++) {
        if (coverage[y * width + x] >= 0.5) area++;
        double[] cell = {coverage[y * width + x], coverage[y * width + x + 1],
            coverage[(y + 1) * width + x + 1], coverage[(y + 1) * width + x]};
        double[][] corner = {{x, y}, {x + 1, y}, {x + 1, y + 1}, {x, y + 1}};
        List<double[]> crossings = new ArrayList<>();
        for (int e = 0; e < 4; e++) {
          int f = (e + 1) % 4;
          if ((cell[e] >= 0.5) != (cell[f] >= 0.5)) {
            double t = (0.5 - cell[e]) / (cell[f] - cell[e]);
            crossings.add(new double[]{corner[e][0] + t * (corner[f][0] - corner[e][0]),
                corner[e][1] + t * (corner[f][1] - corner[e][1])});
          }
        }
        for (int i = 0; i + 1 < crossings.size(); i += 2) {
          length += Math.hypot(crossings.get(i)[0] - crossings.get(i + 1)[0],
              crossings.get(i)[1] - crossings.get(i + 1)[1]);
        }
      }
    }
    return length / Math.sqrt(Math.max(area, 1));
  }

  // ---------------------------------------------------------------- the sprite and the scaling

  private static BufferedImage sample() {
    BufferedImage image =
        new BufferedImage(SAMPLE[0].length(), SAMPLE.length, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < SAMPLE.length; y++) {
      for (int x = 0; x < SAMPLE[y].length(); x++) {
        char at = SAMPLE[y].charAt(x);
        image.setRGB(x, y, at == '#' ? BODY : at == 'o' ? SKIN : 0x00000000);
      }
    }
    return image;
  }

  /** A filled circle, whose outline is diagonal everywhere and whose true length is known. */
  private static BufferedImage disc(int size) {
    BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
    double centre = (size - 1) / 2.0, radius = size / 2.0 - 1;
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        boolean inside = Math.hypot(x - centre, y - centre) <= radius;
        image.setRGB(x, y, inside ? BODY : 0x00000000);
      }
    }
    return image;
  }

  /**
   * A margin of nothing around the sprite, without which the solid has open ends.
   * <p>
   * Willy fills his cell: the hat is on the top row and the feet are on the bottom one. The
   * front and back of this solid meet along the outline BECAUSE the depth there is zero, and
   * where the figure runs off the edge of the picture there is no outline to meet along - the
   * shape is cut, not closed, and the two halves end in mid air with a gap between them. You can
   * see into the head from above.
   * <p>
   * A capping strip along the cut would close it flat. A margin closes it the same way as
   * everywhere else, and costs one line: with background all round, the top of the hat is an
   * edge like any other, the depth falls to zero across it, and the surfaces meet. It also
   * leaves room for the scaler to round those corners, which up against the edge it cannot.
   */
  private static int[] passes(String spec) {
    String[] parts = spec.split("[xX*]");
    int[] passes = new int[parts.length];
    for (int i = 0; i < parts.length; i++) {
      passes[i] = Integer.parseInt(parts[i].trim());
    }
    return passes;
  }

  private static BufferedImage padded(BufferedImage source, int margin, boolean talk) {
    boolean touches = false;
    for (int x = 0; x < source.getWidth(); x++) {
      touches |= (source.getRGB(x, 0) >>> 24) >= 128
          || (source.getRGB(x, source.getHeight() - 1) >>> 24) >= 128;
    }
    for (int y = 0; y < source.getHeight(); y++) {
      touches |= (source.getRGB(0, y) >>> 24) >= 128
          || (source.getRGB(source.getWidth() - 1, y) >>> 24) >= 128;
    }
    if (talk) {
      System.out.println(touches
          ? "the sprite runs off the edge of its cell, so a margin is added to close the solid"
          : "the sprite stands clear of its cell, but a margin is added anyway");
    }
    BufferedImage out = new BufferedImage(source.getWidth() + margin * 2,
        source.getHeight() + margin * 2, BufferedImage.TYPE_INT_ARGB);
    java.awt.Graphics2D pen = out.createGraphics();
    pen.drawImage(source, margin, margin, null);
    pen.dispose();
    return out;
  }

  private static BufferedImage xbrz(BufferedImage source, int factor) {
    int w = source.getWidth(), h = source.getHeight();
    int[] in = source.getRGB(0, 0, w, h, null, 0, w);
    // withAlpha, because the sprite's edge IS its alpha edge and the scaler has to round that
    // rather than treat the transparent surround as a colour.
    int[] scaled = new Xbrz(factor, true).scaleImage(in, null, w, h);
    BufferedImage out = new BufferedImage(w * factor, h * factor, BufferedImage.TYPE_INT_ARGB);
    out.setRGB(0, 0, w * factor, h * factor, scaled, 0, w * factor);
    return out;
  }

  private static BufferedImage nearest(BufferedImage source, int factor) {
    int w = source.getWidth() * factor, h = source.getHeight() * factor;
    BufferedImage out = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        out.setRGB(x, y, source.getRGB(x / factor, y / factor));
      }
    }
    return out;
  }

  private static double[] coverageOf(BufferedImage image) {
    int w = image.getWidth(), h = image.getHeight();
    double[] coverage = new double[w * h];
    for (int y = 0; y < h; y++) {
      for (int x = 0; x < w; x++) {
        coverage[y * w + x] = ((image.getRGB(x, y) >>> 24) & 0xFF) / 255.0;
      }
    }
    return coverage;
  }

  /**
   * Closes the holes that are detail rather than shape, before any of this treats them as shape.
   * <p>
   * Willy's eye is one pixel of background inside his head. Left alone it is a hole in the
   * silhouette, so the solid comes out with a tunnel bored through its skull - and worse, the
   * distance field measures away from it, so the whole face is told it is near an edge and stays
   * thin. An eye is something drawn ON a face, and the way to keep it is in the colour.
   * <p>
   * What counts as detail is size: a hole smaller than about two pixels of the ORIGINAL sprite
   * across. The gap between two legs is far bigger than that and survives, which it must - that
   * one really is shape. Only holes with no way out to the edge of the picture are considered,
   * so the background around the figure is never touched.
   *
   * @param across how wide a hole may be, in scaled pixels, and still be called detail
   */
  private static boolean[] fillSmallHoles(double[] coverage, int width, int height, double across,
                                          boolean talk) {
    boolean[] filled = new boolean[coverage.length];
    double limit = across * across;
    boolean[] seen = new boolean[width * height];
    java.util.ArrayDeque<Integer> queue = new java.util.ArrayDeque<>();
    // Everything the outside can be reached from is background, however far in it reaches.
    for (int x = 0; x < width; x++) {
      consider(coverage, seen, queue, x, 0, width);
      consider(coverage, seen, queue, x, height - 1, width);
    }
    for (int y = 0; y < height; y++) {
      consider(coverage, seen, queue, 0, y, width);
      consider(coverage, seen, queue, width - 1, y, width);
    }
    spread(coverage, seen, queue, width, height);

    // What is left uncovered and unreached is enclosed: a hole.
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int at = y * width + x;
        if (coverage[at] >= 0.5 || seen[at]) {
          continue;
        }
        List<Integer> hole = new ArrayList<>();
        java.util.ArrayDeque<Integer> walk = new java.util.ArrayDeque<>();
        seen[at] = true;
        walk.add(at);
        while (!walk.isEmpty()) {
          int here = walk.poll();
          hole.add(here);
          int hx = here % width, hy = here / width;
          for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
            consider(coverage, seen, walk, hx + step[0], hy + step[1], width);
          }
        }
        if (hole.size() <= limit) {
          for (int one : hole) {
            coverage[one] = 1;
            filled[one] = true;
          }
        }
        if (talk) {
          System.out.printf("%s a hole of %d px (limit %.0f), which is %s%n",
              hole.size() <= limit ? "filled" : "kept", hole.size(), limit,
              hole.size() <= limit ? "detail, not shape" : "shape");
        }
      }
    }
    return filled;
  }

  private static void consider(double[] coverage, boolean[] seen,
                               java.util.Deque<Integer> queue, int x, int y, int width) {
    if (x < 0 || y < 0 || x >= width || y * width + x >= coverage.length) {
      return;
    }
    int at = y * width + x;
    if (!seen[at] && coverage[at] < 0.5) {
      seen[at] = true;
      queue.add(at);
    }
  }

  private static void spread(double[] coverage, boolean[] seen, java.util.Deque<Integer> queue,
                             int width, int height) {
    while (!queue.isEmpty()) {
      int here = queue.poll();
      int x = here % width, y = here / width;
      for (int[] step : new int[][]{{1, 0}, {-1, 0}, {0, 1}, {0, -1}}) {
        consider(coverage, seen, queue, x + step[0], y + step[1], width);
      }
    }
  }

  // ------------------------------------------------------------------------ the distance field

  /**
   * How far each covered point is from the outline, in pixels, exactly - Felzenszwalb and
   * Huttenlocher's transform, which is the squared distance along one axis and then the other.
   * <p>
   * Half a pixel comes off at the end because the outline sits between a covered cell and an
   * uncovered one, not on top of either.
   */
  private static double[] distanceInside(double[] coverage, int width, int height) {
    double far = 1e12;
    double[] squared = new double[width * height];
    for (int i = 0; i < squared.length; i++) {
      squared[i] = coverage[i] >= 0.5 ? far : 0;
    }
    double[] column = new double[Math.max(width, height)];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) column[y] = squared[y * width + x];
      double[] done = transform(column, height);
      for (int y = 0; y < height; y++) squared[y * width + x] = done[y];
    }
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) column[x] = squared[y * width + x];
      double[] done = transform(column, width);
      for (int x = 0; x < width; x++) squared[y * width + x] = done[x];
    }
    double[] distance = new double[width * height];
    for (int i = 0; i < distance.length; i++) {
      distance[i] = Math.max(0, Math.sqrt(squared[i]) - 0.5);
    }
    return distance;
  }

  /**
   * How far every point is from the nearest piece of filled-in detail, so the detail can be put
   * back as something other than a hole.
   */
  private static double[] distanceFrom(boolean[] seeds, int width, int height) {
    boolean any = false;
    for (boolean seed : seeds) {
      any |= seed;
    }
    double[] distance = new double[seeds.length];
    if (!any) {
      java.util.Arrays.fill(distance, Double.MAX_VALUE);
      return distance;
    }
    double far = 1e12;
    double[] squared = new double[seeds.length];
    for (int i = 0; i < squared.length; i++) {
      squared[i] = seeds[i] ? 0 : far;
    }
    double[] line = new double[Math.max(width, height)];
    for (int x = 0; x < width; x++) {
      for (int y = 0; y < height; y++) line[y] = squared[y * width + x];
      double[] done = transform(line, height);
      for (int y = 0; y < height; y++) squared[y * width + x] = done[y];
    }
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) line[x] = squared[y * width + x];
      double[] done = transform(line, width);
      for (int x = 0; x < width; x++) squared[y * width + x] = done[x];
    }
    for (int i = 0; i < distance.length; i++) {
      distance[i] = Math.sqrt(squared[i]);
    }
    return distance;
  }

  /** The one-dimensional squared distance transform: the lower envelope of a set of parabolas. */
  private static double[] transform(double[] f, int n) {
    int[] v = new int[n];
    double[] z = new double[n + 1];
    double[] out = new double[n];
    int k = 0;
    v[0] = 0;
    z[0] = -1e20;
    z[1] = 1e20;
    for (int q = 1; q < n; q++) {
      double s = ((f[q] + q * q) - (f[v[k]] + v[k] * v[k])) / (2.0 * q - 2.0 * v[k]);
      while (s <= z[k]) {
        k--;
        s = ((f[q] + q * q) - (f[v[k]] + v[k] * v[k])) / (2.0 * q - 2.0 * v[k]);
      }
      k++;
      v[k] = q;
      z[k] = s;
      z[k + 1] = 1e20;
    }
    k = 0;
    for (int q = 0; q < n; q++) {
      while (z[k + 1] < q) k++;
      out[q] = (q - v[k]) * (q - v[k]) + f[v[k]];
    }
    return out;
  }

  /**
   * The radius of the largest inscribed disc that COVERS each point, which is not the same as
   * the disc centred on it. A point half way along a two-pixel arm is one pixel from the edge and
   * belongs to a disc of radius one; the same point measured against the figure would be told it
   * belongs to the torso's disc of radius three, and the arm would be inflated as though it were
   * one.
   * <p>
   * Straight out of the definition, every point against every disc. That is quadratic, which is
   * nothing at sixty-four squared and would want a proper sweep at sprite-sheet sizes.
   */
  private static double[] localThickness(double[] distance, int width, int height) {
    List<int[]> centres = new ArrayList<>();
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        if (distance[y * width + x] > 0) centres.add(new int[]{x, y});
      }
    }
    double[] thickness = new double[width * height];
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        double best = distance[y * width + x];
        for (int[] centre : centres) {
          double radius = distance[centre[1] * width + centre[0]];
          if (radius <= best) continue;
          double dx = centre[0] - x, dy = centre[1] - y;
          if (dx * dx + dy * dy <= radius * radius) best = radius;
        }
        thickness[y * width + x] = best;
      }
    }
    return thickness;
  }

  /**
   * Averages a field over its neighbours, counting only what is inside the figure.
   * <p>
   * Outside cells are left out rather than counted as zero: taken in, they would drag every
   * value near the rim down towards nothing and pinch the whole outline inwards.
   */
  private static void soften(double[] field, double[] coverage, int width, int height,
                             int passes) {
    for (int pass = 0; pass < passes; pass++) {
      double[] next = field.clone();
      for (int y = 0; y < height; y++) {
        for (int x = 0; x < width; x++) {
          if (coverage[y * width + x] < 0.5) {
            continue;
          }
          double sum = 0;
          int counted = 0;
          for (int dy = -1; dy <= 1; dy++) {
            for (int dx = -1; dx <= 1; dx++) {
              int nx = x + dx, ny = y + dy;
              if (nx < 0 || ny < 0 || nx >= width || ny >= height) continue;
              if (coverage[ny * width + nx] < 0.5) continue;
              sum += field[ny * width + nx];
              counted++;
            }
          }
          if (counted > 0) {
            next[y * width + x] = sum / counted;
          }
        }
      }
      System.arraycopy(next, 0, field, 0, field.length);
    }
  }

  // ------------------------------------------------------------------------------- the outline

  /**
   * The outline as lines rather than as pixels, one segment per cell the half-coverage level
   * crosses. This is what the mesh below is really built on, and what would be worth handing
   * straight to a modeller.
   */
  private static void writeContour(double[] coverage, int width, int height, Path file)
      throws Exception {
    StringBuilder paths = new StringBuilder();
    for (int y = 0; y + 1 < height; y++) {
      for (int x = 0; x + 1 < width; x++) {
        double[] cell = {coverage[y * width + x], coverage[y * width + x + 1],
            coverage[(y + 1) * width + x + 1], coverage[(y + 1) * width + x]};
        double[][] corner = {{x, y}, {x + 1, y}, {x + 1, y + 1}, {x, y + 1}};
        List<double[]> crossings = new ArrayList<>();
        for (int e = 0; e < 4; e++) {
          int f = (e + 1) % 4;
          if ((cell[e] >= 0.5) != (cell[f] >= 0.5)) {
            double t = (0.5 - cell[e]) / (cell[f] - cell[e]);
            crossings.add(new double[]{corner[e][0] + t * (corner[f][0] - corner[e][0]),
                corner[e][1] + t * (corner[f][1] - corner[e][1])});
          }
        }
        for (int i = 0; i + 1 < crossings.size(); i += 2) {
          paths.append(String.format("M %.3f %.3f L %.3f %.3f ", crossings.get(i)[0],
              crossings.get(i)[1], crossings.get(i + 1)[0], crossings.get(i + 1)[1]));
        }
      }
    }
    try (PrintWriter writer = new PrintWriter(file.toFile(), "UTF-8")) {
      writer.printf("<svg xmlns='http://www.w3.org/2000/svg' viewBox='0 0 %d %d'>%n", width, height);
      writer.printf("<path d='%s' stroke='black' fill='none' stroke-width='0.15'/>%n", paths);
      writer.println("</svg>");
    }
  }

  // ---------------------------------------------------------------------------------- the mesh

  /**
   * Front flat, back bulged, and nothing around the rim: the depth goes to zero at the outline,
   * so the two surfaces meet there on their own and the solid closes.
   * <p>
   * Each cell of the grid is clipped against "coverage at least a half" - a plain polygon clip,
   * no case table - so the silhouette follows the same half-coverage line the outline does,
   * rather than the edges of whole pixels. That is the step that keeps the smoothing.
   */
  private static List<double[]> build(double[] coverage, double[] depth, int width, int height,
                                      boolean mirrored) {
    List<double[]> triangles = new ArrayList<>();
    for (int y = 0; y + 1 < height; y++) {
      for (int x = 0; x + 1 < width; x++) {
        double[][] square = {
            {x, y, coverage[y * width + x], depth[y * width + x]},
            {x + 1, y, coverage[y * width + x + 1], depth[y * width + x + 1]},
            {x + 1, y + 1, coverage[(y + 1) * width + x + 1], depth[(y + 1) * width + x + 1]},
            {x, y + 1, coverage[(y + 1) * width + x], depth[(y + 1) * width + x]}};
        List<double[]> inside = clip(square);
        if (inside.size() < 3) continue;
        for (int i = 1; i + 1 < inside.size(); i++) {
          double[] a = inside.get(0), b = inside.get(i), c = inside.get(i + 1);
          // Mirrored: the same bulge forwards as backwards, so the section is a whole ellipse and
          // not half of one against a flat back. Either way the two halves meet at the outline,
          // where both are zero, so the solid closes there.
          double front = mirrored ? -1 : 0;
          triangles.add(new double[]{a[0], a[1], front * a[3], b[0], b[1], front * b[3],
              c[0], c[1], front * c[3]});
          triangles.add(new double[]{a[0], a[1], a[3], c[0], c[1], c[3], b[0], b[1], b[3]});
        }
      }
    }
    return triangles;
  }

  /** The part of a cell that is covered, cut where the coverage crosses a half. */
  private static List<double[]> clip(double[][] polygon) {
    List<double[]> out = new ArrayList<>();
    for (int i = 0; i < polygon.length; i++) {
      double[] here = polygon[i], next = polygon[(i + 1) % polygon.length];
      boolean in = here[2] >= 0.5, nextIn = next[2] >= 0.5;
      if (in) out.add(here);
      if (in != nextIn) {
        double t = (0.5 - here[2]) / (next[2] - here[2]);
        // Depth ZERO, not the depth interpolated between the two corners. This point is ON the
        // outline, and the outline is where the distance from the outline is nothing - so it is
        // where the front and the back of the solid have to meet.
        //
        // Interpolating it instead was the reason they did not. The corner just inside is half a
        // pixel in and already carries a real depth; the corner outside carries zero because
        // nothing was computed there; halfway between them is half of a real depth. So the front
        // stopped at minus that and the back at plus it, and the figure was open along its whole
        // silhouette - a slot two units wide that you could see straight into.
        out.add(new double[]{here[0] + t * (next[0] - here[0]), here[1] + t * (next[1] - here[1]),
            0.5, 0});
      }
    }
    return out;
  }

  /**
   * The solid as a mesh, centred on nothing in particular and carrying a normal per face.
   * <p>
   * The normals are written rather than left to the reader: a loader that finds none either
   * shades everything the same - which hides the whole point of the bulge - or guesses them from
   * a vertex order it cannot know the meaning of.
   */
  /**
   * Where a vertex is, to enough places that two of them at the same place agree on it.
   * <p>
   * Zero is forced positive. The front of the solid sits at minus the depth and the back at plus
   * it, and along the outline the depth is zero - so the two surfaces arrive at the same point
   * from opposite sides, one of them carrying negative zero. They compare equal as numbers and
   * print differently, so the rim of the figure went unwelded and the solid was open along its
   * entire outline: six hundred and seventy-six edges with nothing on the other side.
   */
  static String key(double x, double y, double z) {
    return String.format("%.4f/%.4f/%.4f", x == 0 ? 0 : x, y == 0 ? 0 : y, z == 0 ? 0 : z);
  }

  /**
   * One normal per VERTEX, averaged over the faces meeting there, rather than one per face.
   * <p>
   * This is the difference between a surface that looks smooth and one that looks like a
   * disco ball, and it is not a property of the geometry at all - the same triangles, told
   * differently. A face normal makes every triangle a flat facet with a hard edge against its
   * neighbours, so a bulge made of two thousand small triangles is seen as two thousand small
   * flats. Averaged at the shared corner, the shading crosses the joins and the eye reads the
   * curve the triangles are approximating instead of the approximation.
   * <p>
   * The faces are added unnormalised on purpose: the cross product's length is twice the
   * triangle's area, so a big face has its due weight against a sliver.
   */
  static java.util.Map<String, double[]> smoothNormals(List<double[]> triangles) {
    java.util.Map<String, double[]> normals = new java.util.HashMap<>();
    for (double[] triangle : triangles) {
      double[] a = {triangle[0], triangle[1], triangle[2]};
      double[] b = {triangle[3], triangle[4], triangle[5]};
      double[] c = {triangle[6], triangle[7], triangle[8]};
      double[] face = cross(a, b, c);
      for (double[] corner : new double[][]{a, b, c}) {
        double[] sum = normals.computeIfAbsent(key(corner[0], corner[1], corner[2]),
            unused -> new double[3]);
        sum[0] += face[0];
        sum[1] += face[1];
        sum[2] += face[2];
      }
    }
    for (double[] sum : normals.values()) {
      double length = Math.sqrt(sum[0] * sum[0] + sum[1] * sum[1] + sum[2] * sum[2]);
      if (length > 1e-12) {
        sum[0] /= length;
        sum[1] /= length;
        sum[2] /= length;
      }
    }
    return normals;
  }

  /** The solid as a mesh, welded, with a normal at every vertex. */
  static void writeObj(List<double[]> triangles, Path file) throws Exception {
    java.util.Map<String, double[]> normals = smoothNormals(triangles);
    java.util.Map<String, Integer> numbered = new java.util.LinkedHashMap<>();
    List<double[]> points = new ArrayList<>();
    for (double[] triangle : triangles) {
      for (int i = 0; i < 3; i++) {
        double x = triangle[i * 3], y = triangle[i * 3 + 1], z = triangle[i * 3 + 2];
        if (numbered.putIfAbsent(key(x, y, z), numbered.size() + 1) == null) {
          points.add(new double[]{x, y, z});
        }
      }
    }
    try (PrintWriter writer = new PrintWriter(file.toFile(), "UTF-8")) {
      writer.println("# sprite inflated to a solid, mirrored, welded, smooth normals");
      // Y flipped, because an image counts rows downwards and a model counts them up.
      for (double[] point : points) {
        writer.printf("v %.4f %.4f %.4f%n", point[0], -point[1], point[2]);
      }
      for (double[] point : points) {
        double[] normal = normals.get(key(point[0], point[1], point[2]));
        writer.printf("vn %.4f %.4f %.4f%n", normal[0], -normal[1], normal[2]);
      }
      for (double[] triangle : triangles) {
        int a = numbered.get(key(triangle[0], triangle[1], triangle[2]));
        int b = numbered.get(key(triangle[3], triangle[4], triangle[5]));
        int c = numbered.get(key(triangle[6], triangle[7], triangle[8]));
        writer.printf("f %d//%d %d//%d %d//%d%n", a, a, b, b, c, c);
      }
      // An edge belonging to one face only is a hole: nothing on the other side of it. A closed
      // solid has none, and this is the whole of "you cannot see inside it" as a number.
      java.util.Map<Long, Integer> edges = new java.util.HashMap<>();
      for (double[] triangle : triangles) {
        int a = numbered.get(key(triangle[0], triangle[1], triangle[2]));
        int b = numbered.get(key(triangle[3], triangle[4], triangle[5]));
        int c = numbered.get(key(triangle[6], triangle[7], triangle[8]));
        for (int[] pair : new int[][]{{a, b}, {b, c}, {c, a}}) {
          long id = (long) Math.min(pair[0], pair[1]) << 32 | Math.max(pair[0], pair[1]);
          edges.merge(id, 1, Integer::sum);
        }
      }
      long open = edges.values().stream().filter(count -> count < 2).count();
      System.out.printf("  %s: %d triangles, %d welded vertices, %d open edges%n",
          file.getFileName(), triangles.size(), points.size(), open);
    }
  }

  // ------------------------------------------------------------------------------- the picture

  /** The same figure from four angles, so the bulge can be seen rather than taken on trust. */
  private static BufferedImage turntable(List<double[]> mesh, Fields fields, String label) {
    BufferedImage flat = fields.scaled();
    int width = fields.width(), height = fields.height();
    int size = 260;
    double[] angles = {0, 25, 50, 90};
    BufferedImage strip =
        new BufferedImage(size * (angles.length + 1), size, BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D pen = strip.createGraphics();
    pen.setColor(new java.awt.Color(0x101014));
    pen.fillRect(0, 0, strip.getWidth(), size);
    // The flat sprite first, for comparison with what it turns into.
    pen.setRenderingHint(java.awt.RenderingHints.KEY_INTERPOLATION,
        java.awt.RenderingHints.VALUE_INTERPOLATION_NEAREST_NEIGHBOR);
    int side = (int) (size * 0.8);
    pen.drawImage(flat, (size - side * width / height) / 2, (size - side) / 2,
        side * width / height, side, null);
    pen.setColor(java.awt.Color.WHITE);
    pen.drawString(label, 8, size - 10);
    pen.dispose();
    for (int i = 0; i < angles.length; i++) {
      render(mesh, smoothNormals(mesh), fields, Math.toRadians(angles[i]), strip,
          size * (i + 1), size);
    }
    return strip;
  }

  /** A z-buffer and a lamp: enough to see a shape, and no more. */
  private static void render(List<double[]> mesh, java.util.Map<String, double[]> normals,
                             Fields fields, double yaw, BufferedImage target, int left, int size) {
    int width = fields.width(), height = fields.height();
    double[] zbuffer = new double[size * size];
    java.util.Arrays.fill(zbuffer, Double.NEGATIVE_INFINITY);
    int[] pixels = new int[size * size];
    java.util.Arrays.fill(pixels, 0x101014);

    double scale = size * 0.8 / height;
    double cx = width / 2.0, cy = height / 2.0;
    double cos = Math.cos(yaw), sin = Math.sin(yaw);

    for (double[] triangle : mesh) {
      double[][] point = new double[3][];
      for (int i = 0; i < 3; i++) {
        double x = triangle[i * 3] - cx, y = triangle[i * 3 + 1] - cy, z = triangle[i * 3 + 2];
        // Turn about the upright axis, then straight down the camera: no perspective, because a
        // turntable of a flat-fronted thing is about the silhouette, not about the vanishing.
        double rx = x * cos + z * sin, rz = -x * sin + z * cos;
        point[i] = new double[]{size / 2.0 + rx * scale, size / 2.0 + y * scale, rz};
      }
      // A normal per corner, turned with the model, so the light crosses the joins between
      // triangles instead of stopping at each one. Shading per face here would show facets that
      // the mesh handed to a viewer does not have, which is a picture of the wrong thing.
      double[][] corner = new double[3][];
      for (int i = 0; i < 3; i++) {
        double[] at = normals.get(key(triangle[i * 3], triangle[i * 3 + 1], triangle[i * 3 + 2]));
        corner[i] = at == null ? new double[]{0, 0, 1}
            : new double[]{at[0] * cos + at[2] * sin, at[1], -at[0] * sin + at[2] * cos};
      }
      // The sprite's own colours, sampled where the triangle sits on it.
      int rgb = colourFor(fields, (triangle[0] + triangle[3] + triangle[6]) / 3,
          (triangle[1] + triangle[4] + triangle[7]) / 3);
      raster(point, corner, rgb, pixels, zbuffer, size);
    }
    for (int y = 0; y < size; y++) {
      for (int x = 0; x < size; x++) {
        target.setRGB(left + x, y, pixels[y * size + x]);
      }
    }
  }

  private static int clamp(double value) {
    return (int) Math.min(255, Math.max(0, value));
  }

  private static double[] cross(double[] a, double[] b, double[] c) {
    double ux = b[0] - a[0], uy = b[1] - a[1], uz = b[2] - a[2];
    double vx = c[0] - a[0], vy = c[1] - a[1], vz = c[2] - a[2];
    return new double[]{uy * vz - uz * vy, uz * vx - ux * vz, ux * vy - uy * vx};
  }

  private static void raster(double[][] point, double[][] corner, int rgb, int[] pixels,
                             double[] zbuffer, int size) {
    int minX = (int) Math.max(0, Math.floor(Math.min(point[0][0], Math.min(point[1][0], point[2][0]))));
    int maxX = (int) Math.min(size - 1, Math.ceil(Math.max(point[0][0], Math.max(point[1][0], point[2][0]))));
    int minY = (int) Math.max(0, Math.floor(Math.min(point[0][1], Math.min(point[1][1], point[2][1]))));
    int maxY = (int) Math.min(size - 1, Math.ceil(Math.max(point[0][1], Math.max(point[1][1], point[2][1]))));
    double area = edge(point[0], point[1], point[2]);
    if (Math.abs(area) < 1e-9) return;
    for (int y = minY; y <= maxY; y++) {
      for (int x = minX; x <= maxX; x++) {
        double[] at = {x + 0.5, y + 0.5};
        double w0 = edge(point[1], point[2], at) / area;
        double w1 = edge(point[2], point[0], at) / area;
        double w2 = edge(point[0], point[1], at) / area;
        if (w0 < 0 || w1 < 0 || w2 < 0) continue;
        double z = w0 * point[0][2] + w1 * point[1][2] + w2 * point[2][2];
        if (z > zbuffer[y * size + x]) {
          double nx = w0 * corner[0][0] + w1 * corner[1][0] + w2 * corner[2][0];
          double ny = w0 * corner[0][1] + w1 * corner[1][1] + w2 * corner[2][1];
          double nz = w0 * corner[0][2] + w1 * corner[1][2] + w2 * corner[2][2];
          double length = Math.sqrt(nx * nx + ny * ny + nz * nz);
          if (length > 1e-9) {
            nx /= length;
            ny /= length;
            nz /= length;
          }
          if (nz < 0) {
            nx = -nx;
            ny = -ny;
            nz = -nz;
          }
          // From over the viewer's left shoulder. Straight down the camera lights every part of
          // a bulge almost equally and hands back the flat sprite it started as.
          double shade = 0.22 + 0.78 * Math.max(0, nx * -0.42 + ny * -0.50 + nz * 0.76);
          zbuffer[y * size + x] = z;
          pixels[y * size + x] = (clamp(((rgb >> 16) & 0xFF) * shade) << 16)
              | (clamp(((rgb >> 8) & 0xFF) * shade) << 8) | clamp((rgb & 0xFF) * shade);
        }
      }
    }
  }

  private static double edge(double[] a, double[] b, double[] c) {
    return (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0]);
  }

  private static void writeGrey(double[] field, int width, int height, double top, Path file)
      throws Exception {
    BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
    for (int y = 0; y < height; y++) {
      for (int x = 0; x < width; x++) {
        int v = (int) Math.round(255 * Math.min(1, field[y * width + x] / Math.max(top, 1e-9)));
        image.setRGB(x, y, (v << 16) | (v << 8) | v);
      }
    }
    ImageIO.write(image, "png", file.toFile());
  }

  private static BufferedImage stack(List<BufferedImage> rows) {
    int width = rows.get(0).getWidth(), height = rows.get(0).getHeight();
    BufferedImage all = new BufferedImage(width, height * rows.size(), BufferedImage.TYPE_INT_RGB);
    java.awt.Graphics2D pen = all.createGraphics();
    for (int i = 0; i < rows.size(); i++) {
      pen.drawImage(rows.get(i), 0, i * height, null);
    }
    pen.dispose();
    return all;
  }
}
