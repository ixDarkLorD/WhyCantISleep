// The landing page's motion: a night sky of twinkling stars, sections revealed as they come into view, and the
// gameplay video's chapter chips. Runs on every page load, instant navigation too; does nothing off the landing page.
(function () {
  const quiet = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
  let stop = [];

  // The night sky behind the hero: stars of a few sizes, twinkling slowly, a few drifting.
  function stars(canvas) {
    const pen = canvas.getContext("2d");
    let width = 0, height = 0, dots = [], frame = 0;
    function size() {
      const scale = window.devicePixelRatio || 1;
      width = canvas.clientWidth; height = canvas.clientHeight;
      canvas.width = width * scale; canvas.height = height * scale;
      pen.setTransform(scale, 0, 0, scale, 0, 0);
      const count = Math.round((width * height) / 5200);
      dots = Array.from({length: count}, () => ({
        x: Math.random() * width, y: Math.random() * height * 0.85,
        r: Math.random() < 0.08 ? 1.6 : Math.random() * 0.9 + 0.3,
        phase: Math.random() * Math.PI * 2, speed: 0.4 + Math.random() * 1.2,
      }));
    }
    function draw(time) {
      pen.clearRect(0, 0, width, height);
      for (const dot of dots) {
        const glow = quiet ? 0.7 : 0.45 + 0.55 * (0.5 + 0.5 * Math.sin(dot.phase + time * 0.001 * dot.speed));
        pen.globalAlpha = glow;
        pen.fillStyle = dot.r > 1.4 ? "#f2dd9b" : "#e6e0f2";
        pen.fillRect(dot.x, dot.y, dot.r, dot.r);
        if (dot.r > 1.4) {    // the bright ones, a cross of light
          pen.globalAlpha = glow * 0.35;
          pen.fillRect(dot.x - 2, dot.y + dot.r / 2 - 0.25, dot.r + 4, 0.5);
          pen.fillRect(dot.x + dot.r / 2 - 0.25, dot.y - 2, 0.5, dot.r + 4);
        }
      }
      pen.globalAlpha = 1;
      if (!quiet) frame = requestAnimationFrame(draw);
    }
    size();
    frame = requestAnimationFrame(draw);
    window.addEventListener("resize", size);
    stop.push(() => { cancelAnimationFrame(frame); window.removeEventListener("resize", size); });
  }

  // Sections come in as they are scrolled to.
  function reveal() {
    const items = document.querySelectorAll("[data-reveal]");
    if (!("IntersectionObserver" in window) || quiet) { items.forEach((item) => item.classList.add("is-in")); return; }
    const watch = new IntersectionObserver((entries) => {
      entries.forEach((entry) => { if (entry.isIntersecting) { entry.target.classList.add("is-in"); watch.unobserve(entry.target); } });
    }, {rootMargin: "0px 0px -8% 0px"});
    items.forEach((item) => watch.observe(item));
    stop.push(() => watch.disconnect());
  }

  // The gameplay video: its chips light up with the part playing and jump to theirs. With reduced motion it waits
  // on its poster for a click.
  function demo(figure) {
    const video = figure.querySelector("video");
    const chips = [...figure.querySelectorAll("[data-at]")];
    if (!video) return;
    if (quiet) { video.removeAttribute("autoplay"); video.pause(); video.controls = true; }
    function mark() {
      let on = chips[0];
      chips.forEach((chip) => { if (video.currentTime >= parseFloat(chip.dataset.at)) on = chip; });
      chips.forEach((chip) => chip.classList.toggle("is-on", chip === on));
    }
    chips.forEach((chip) => chip.addEventListener("click", () => {
      video.currentTime = parseFloat(chip.dataset.at);
      video.play().catch(() => {});
      mark();
    }));
    video.addEventListener("timeupdate", mark);
    mark();
    stop.push(() => video.removeEventListener("timeupdate", mark));
  }

  function setup() {
    stop.forEach((end) => end());
    stop = [];
    if (!document.querySelector(".wh")) return;
    document.querySelectorAll(".wh-stars").forEach(stars);
    reveal();
    document.querySelectorAll("[data-demo]").forEach(demo);
  }

  if (window.document$ && window.document$.subscribe) window.document$.subscribe(setup);
  else document.addEventListener("DOMContentLoaded", setup);
})();
