class Cook < Formula
  desc "Recipe driven configuration tool for the files a repository shares"
  homepage "https://lmarqs.github.io/the-cook/"
  version "11.0.0"
  license "Apache-2.0"

  on_macos do
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-macos-arm64.tar.gz"
      sha256 "7f44618b44b27757b5455eaca113e01c246faa023bdac2b106b9cbf896509806"
    end
  end

  on_linux do
    on_intel do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-x64.tar.gz"
      sha256 "a81dfdad1b7c51ad1e4776cdd1ef3eba12f28587d66fbdb99f39147a59f21b2b"
    end
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-arm64.tar.gz"
      sha256 "5ce342f8146a7abaf0ae07f4ef822b85a1d0b4dc51b3b81fd97c3e2f0ac3e12d"
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
