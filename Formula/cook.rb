class Cook < Formula
  desc "Recipe driven configuration tool for the files a repository shares"
  homepage "https://lmarqs.github.io/the-cook/"
  version "12.3.1"
  license "Apache-2.0"

  on_macos do
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-macos-arm64.tar.gz"
      sha256 "d0eb4b1adfab33840f8d52aaf29f74584575d5b4569f059ab3229c0f0619053d"
    end
  end

  on_linux do
    on_intel do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-x64.tar.gz"
      sha256 "4b91eb9560a850b545161ba09b985fc1ff07038ba51040712d14aa45fb3ef053"
    end
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-arm64.tar.gz"
      sha256 "07677b08810a7951e818eba2a0946c995a9e119b66eb7c6bc2de1acac9370456"
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
