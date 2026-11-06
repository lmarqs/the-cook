class Cook < Formula
  desc "Recipe driven configuration tool for the files a repository shares"
  homepage "https://lmarqs.github.io/the-cook/"
  version "11.0.1"
  license "Apache-2.0"

  on_macos do
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-macos-arm64.tar.gz"
      sha256 "bb4d31fe7df0d7677030354d0689ad864c2fdb5ff85764ca40349e6f70c570c4"
    end
  end

  on_linux do
    on_intel do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-x64.tar.gz"
      sha256 "6aa2d7a74dfb1c75ad27b1f4fc2e6945e7851467042e2e98a9c08949e9baaa75"
    end
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-arm64.tar.gz"
      sha256 "9a3213dba98096050f103352260fa246901df8f85504ffef58f7e1ffff6039c8"
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
