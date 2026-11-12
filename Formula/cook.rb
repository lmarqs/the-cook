class Cook < Formula
  desc "Recipe driven configuration tool for the files a repository shares"
  homepage "https://lmarqs.github.io/the-cook/"
  version "12.0.0"
  license "Apache-2.0"

  on_macos do
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-macos-arm64.tar.gz"
      sha256 "83cda31b98e5a2fcced855f93a7172145e1ce2b4a59a380ced49fec1caa3fafd"
    end
  end

  on_linux do
    on_intel do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-x64.tar.gz"
      sha256 "02034ff74898daeb6bbb4ba43a2621ca60585b9e842fe729b099bd6169438fd4"
    end
    on_arm do
      url "https://github.com/lmarqs/the-cook/releases/download/v#{version}/the-cook-#{version}-linux-arm64.tar.gz"
      sha256 "a72d94bd8d223a5bc450e8f7ea00e6ff90d5591f56739e2d74d53db11461ad2e"
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
