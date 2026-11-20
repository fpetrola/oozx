#include <dlfcn.h>
#include <stdio.h>
#include <stdlib.h>
#include <time.h>
struct retro_game_info { const char *path; const void *data; size_t size; const char *meta; };
int main(int argc, char **argv) {
  void *so = dlopen(argv[1], RTLD_NOW);
  if (!so) { puts(dlerror()); return 1; }
  void (*init)(void) = dlsym(so, "retro_init");
  char (*load)(const struct retro_game_info *) = dlsym(so, "retro_load_game");
  int (*play)(int, int) = dlsym(so, "retro_play_recording");
  FILE *f = fopen(argv[2], "rb"); fseek(f, 0, SEEK_END); long n = ftell(f); rewind(f);
  void *d = malloc(n); if (fread(d, 1, n, f) != n) return 2;
  struct retro_game_info g = { argv[2], d, n, 0 };
  init();
  if (!load(&g)) { puts("load failed"); return 3; }
  struct timespec a, b; clock_gettime(CLOCK_MONOTONIC, &a);
  int runs = play(atoi(argv[3]), atoi(argv[4]));
  clock_gettime(CLOCK_MONOTONIC, &b);
  double s = (b.tv_sec - a.tv_sec) + (b.tv_nsec - a.tv_nsec) / 1e9;
  printf("sound=%s picture_every=%s frames=%d seconds=%.2f\n", argv[3], argv[4], runs, s);
  return 0;
}
