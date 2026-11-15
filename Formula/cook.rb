class Cook < Formula
  desc "Recipe driven configuration tool for the files a repository shares"
  homepage "https://lmarqs.github.io/the-cook/"
  version "12.2.0"
  license "Apache-2.0"

  on_macos do
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-macos-arm64.tar.gz"
      sha256 "c23e6c24f7cc4048945a5ce241bb026e3125bbf866aa2b6acf7497e696a2429c"
    end
  end

  on_linux do
    on_intel do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-x64.tar.gz"
      sha256 "4c9d4593e36c98320f97ad4a60f090b1aaee48f62c27cd5a711a61225764af74"
    end
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-arm64.tar.gz"
      sha256 "7585cc49bd1b10bc188ca6f18ec36d418d85d7f953c96ca055a56b3b4630050e"
    end
  end

  def install
    if OS.mac?
      (libexec/"cook.app").install Dir["*"]
      bin.write_exec_script libexec/"cook.app/Contents/MacOS/cook"
    else
      libexec.install Dir["*"]
      bin.write_exec_script libexec/"bin/cook"
    end
  end

  test do
    assert_match version.to_s, shell_output("#{bin}/cook --version")
  end
end
